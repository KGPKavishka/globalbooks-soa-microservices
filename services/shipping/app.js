// FIXED: Robust, production-ready Shipping service with RabbitMQ topology, Redis idempotency, retries, JSON logging, metrics, and health endpoints

'use strict';

const express = require('express'); // Express app
const amqp = require('amqplib'); // RabbitMQ client
const redis = require('redis'); // Redis v4 client
const winston = require('winston'); // JSON logging
const promClient = require('prom-client'); // Prometheus metrics
const Ajv = require('ajv'); // JSON Schema validation
const addFormats = require('ajv-formats'); // AJV formats
const path = require('path');
const { randomUUID } = require('crypto');

// -------------------------------------------------------------------------------------
// Configuration
// -------------------------------------------------------------------------------------

// FIXED: Env config with sensible defaults (local dev -> localhost; container prod -> rabbitmq/redis)
const NODE_ENV = process.env.NODE_ENV || 'development';
const IS_PROD = NODE_ENV === 'production';

// RabbitMQ
const RABBITMQ_HOST = process.env.RABBITMQ_HOST || (IS_PROD ? 'rabbitmq' : 'localhost'); // FIXED
const RABBITMQ_PORT = process.env.RABBITMQ_PORT || '5672'; // FIXED
const AMQP_URL = process.env.AMQP_URL || `amqp://guest:guest@${RABBITMQ_HOST}:${RABBITMQ_PORT}`; // FIXED

// Redis
const REDIS_HOST = process.env.REDIS_HOST || (IS_PROD ? 'redis' : 'localhost'); // FIXED
const REDIS_PORT = process.env.REDIS_PORT || '6379'; // FIXED
const REDIS_URL = process.env.REDIS_URL || `redis://${REDIS_HOST}:${REDIS_PORT}`; // FIXED

// Names (exchanges, queues, routing keys) aligned with Payments RabbitConfig.java
const EXCHANGE_MAIN = process.env.SHIPPING_EXCHANGE || 'shipping.exchange'; // FIXED
const EXCHANGE_RETRY = process.env.SHIPPING_RETRY_EXCHANGE || 'shipping.retry.exchange'; // FIXED
const EXCHANGE_DLX = process.env.SHIPPING_DLX || 'shipping.dlx'; // FIXED

const ROUTING_KEY_PROCESS = process.env.SHIPPING_ROUTING_KEY || 'shipping.process'; // FIXED
const ROUTING_KEY_DEAD = process.env.SHIPPING_DEAD_ROUTING_KEY || 'shipping.dead'; // FIXED

const QUEUE_MAIN = process.env.SHIPPING_QUEUE || 'shipping.queue'; // FIXED
const QUEUE_RETRY = process.env.SHIPPING_RETRY_QUEUE || 'shipping.retry.queue'; // FIXED
const QUEUE_DLQ = process.env.SHIPPING_DLQ || 'shipping.dlq'; // FIXED

const RETRY_TTL_MS = Number(process.env.SHIPPING_RETRY_TTL_MS || 5000); // FIXED: delayed redelivery 5s
const MAX_ATTEMPTS = Number(process.env.SHIPPING_MAX_ATTEMPTS || 5); // FIXED: bounded retries
const IDEMP_TTL_SECONDS = Number(process.env.SHIPPING_IDEMP_TTL_SECONDS || 86400); // FIXED: 24h idempotency

const SERVICE_NAME = process.env.SERVICE_NAME || 'shipping-service';
const PORT = Number(process.env.PORT || 3000);

// -------------------------------------------------------------------------------------
// Logger (Winston JSON)
// -------------------------------------------------------------------------------------

// FIXED: Structured JSON logs with defaultMeta; request-specific correlationId via child logger
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  defaultMeta: { service: SERVICE_NAME, env: NODE_ENV },
  format: winston.format.json(),
  transports: [new winston.transports.Console()],
});

// -------------------------------------------------------------------------------------
// Metrics (Prometheus)
// -------------------------------------------------------------------------------------

// FIXED: Register default metrics and custom counters
const register = new promClient.Registry();
promClient.collectDefaultMetrics({ register, labels: { service: SERVICE_NAME } });

const metricProcessed = new promClient.Counter({
  name: 'shipping_events_processed_total',
  help: 'Total number of shipping events successfully processed',
  labelNames: ['status'],
});
const metricDuplicate = new promClient.Counter({
  name: 'shipping_events_duplicate_total',
  help: 'Total number of duplicate shipping events ignored due to idempotency',
});
const metricRetried = new promClient.Counter({
  name: 'shipping_events_retried_total',
  help: 'Total number of shipping events retried',
});
const metricFailed = new promClient.Counter({
  name: 'shipping_events_failed_total',
  help: 'Total number of shipping events sent to DLQ',
  labelNames: ['reason'],
});
const httpRequestDuration = new promClient.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.1, 0.5, 1, 2, 5],
});
register.registerMetric(metricProcessed);
register.registerMetric(metricDuplicate);
register.registerMetric(metricRetried);
register.registerMetric(metricFailed);
register.registerMetric(httpRequestDuration);

// -------------------------------------------------------------------------------------
// Schema Validation (AJV)
// -------------------------------------------------------------------------------------

// FIXED: Compile runtime validation schema
const ajv = new Ajv({ allErrors: true, strict: false });
// FIXED: add JSON Schema 2020-12 meta-schema so Ajv recognizes $schema URL
try {
  // eslint-disable-next-line global-require
  ajv.addMetaSchema(require('ajv/dist/refs/json-schema-2020-12.json'));
} catch (e) {
  // ignore if already added or not found
}
addFormats(ajv);
let validatePaymentEvent;
try {
  const schemaPath = path.join(__dirname, 'schemas', 'payment-event.schema.json');
  // eslint-disable-next-line import/no-dynamic-require, global-require
  const paymentEventSchema = require(schemaPath);
  validatePaymentEvent = ajv.compile(paymentEventSchema);
  logger.info('PaymentEvent schema compiled');
} catch (e) {
  logger.error('Failed to compile PaymentEvent schema', { error: e?.message });
  // Continue startup; events will be DLQ-ed if no schema
}

// FIXED: Throttle noisy Redis reconnect/error logs to prevent log storms when Redis is down
const REDIS_LOG_THROTTLE_MS = Number(process.env.REDIS_LOG_THROTTLE_MS || 10000);
let __redisLastLogTs = 0;
function redisLogThrottled(level, message, meta = {}) {
  const now = Date.now();
  if (now - __redisLastLogTs >= REDIS_LOG_THROTTLE_MS) {
    __redisLastLogTs = now;
    if (typeof logger[level] === 'function') {
      logger[level](message, meta);
    } else {
      logger.log({ level, message, ...meta });
    }
  }
}
// -------------------------------------------------------------------------------------
// Redis (v4) Idempotency
// -------------------------------------------------------------------------------------

// FIXED: Redis v4 client with auto-reconnect
const redisClient = redis.createClient({ url: REDIS_URL });
let isRedisConnected = false;

redisClient.on('ready', () => {
  isRedisConnected = true;
  logger.info('Redis connected');
});
redisClient.on('end', () => {
  isRedisConnected = false;
  logger.warn('Redis connection ended');
});
redisClient.on('reconnecting', () => {
  redisLogThrottled('warn', 'Redis reconnecting...');
});
redisClient.on('error', (err) => {
  isRedisConnected = false;
  redisLogThrottled('error', 'Redis error', { error: err?.message });
});

// -------------------------------------------------------------------------------------
// RabbitMQ Topology and Consumer
// -------------------------------------------------------------------------------------

// FIXED: Topology auto-declaration at startup; robust consumer lifecycle with reconnect/backoff
let amqpConnection = null;
let amqpChannel = null;
let isRabbitConnected = false;
let reconnecting = false;
let backoffMs = 1000; // start at 1s
const MAX_BACKOFF_MS = 30000;

async function setupTopology(ch) {
  // Exchanges
  await ch.assertExchange(EXCHANGE_MAIN, 'topic', { durable: true }); // FIXED
  await ch.assertExchange(EXCHANGE_RETRY, 'direct', { durable: true }); // FIXED
  await ch.assertExchange(EXCHANGE_DLX, 'direct', { durable: true }); // FIXED

  // Queues
  await ch.assertQueue(QUEUE_MAIN, {
    durable: true,
    arguments: {
      'x-dead-letter-exchange': EXCHANGE_DLX, // FIXED
      'x-dead-letter-routing-key': ROUTING_KEY_DEAD, // FIXED
    },
  });
  await ch.assertQueue(QUEUE_RETRY, {
    durable: true,
    arguments: {
      'x-message-ttl': RETRY_TTL_MS, // FIXED
      'x-dead-letter-exchange': EXCHANGE_MAIN, // FIXED
      'x-dead-letter-routing-key': ROUTING_KEY_PROCESS, // FIXED
    },
  });
  await ch.assertQueue(QUEUE_DLQ, {
    durable: true,
  });

  // Bindings
  await ch.bindQueue(QUEUE_MAIN, EXCHANGE_MAIN, ROUTING_KEY_PROCESS); // FIXED
  await ch.bindQueue(QUEUE_RETRY, EXCHANGE_RETRY, ROUTING_KEY_PROCESS); // FIXED
  await ch.bindQueue(QUEUE_DLQ, EXCHANGE_DLX, ROUTING_KEY_DEAD); // FIXED

  logger.info('RabbitMQ topology asserted', {
    exchanges: [EXCHANGE_MAIN, EXCHANGE_RETRY, EXCHANGE_DLX],
    queues: [QUEUE_MAIN, QUEUE_RETRY, QUEUE_DLQ],
  });
}

function getCorrelationIdFromMsg(msg) {
  const headers = (msg?.properties?.headers) || {};
  return (
    headers['x-correlation-id'] ||
    headers['correlationId'] ||
    msg?.properties?.correlationId ||
    randomUUID()
  );
}

function headersWithCorrelation(headers, correlationId) {
  return {
    ...headers,
    'x-correlation-id': correlationId, // FIXED: always propagate
  };
}

async function publishRetry(ch, msg, correlationId, attempts) {
  const headers = headersWithCorrelation((msg.properties.headers || {}), correlationId);
  headers.attempts = attempts + 1;

  // FIXED: publish to retry exchange with same routing key; retry queue routes back to main after TTL
  ch.publish(
    EXCHANGE_RETRY,
    ROUTING_KEY_PROCESS,
    msg.content,
    {
      persistent: true,
      headers,
      correlationId,
      contentType: msg.properties.contentType || 'application/json',
      contentEncoding: msg.properties.contentEncoding,
      timestamp: Date.now(),
    },
  );
  metricRetried.inc();
}

async function publishDLQ(ch, originalMessage, correlationId, attempts, reason, rawContent) {
  const payload = {
    reason,
    attempts,
    originalMessage,
    rawContentBase64: rawContent ? Buffer.from(rawContent).toString('base64') : undefined,
  };

  const headers = {
    'x-correlation-id': correlationId, // FIXED
    attempts,
    reason,
  };

  ch.publish(
    EXCHANGE_DLX,
    ROUTING_KEY_DEAD,
    Buffer.from(JSON.stringify(payload)),
    {
      persistent: true,
      headers,
      correlationId,
      contentType: 'application/json',
      timestamp: Date.now(),
    },
  );
  metricFailed.inc({ reason });
}

async function processMessage(ch, msg) {
  // Ensure ack or nack to avoid infinite re-delivery
  if (!msg) return;

  const correlationId = getCorrelationIdFromMsg(msg);
  const log = logger.child({ correlationId }); // FIXED: per-message defaultMeta

  let content;
  try {
    content = JSON.parse(msg.content.toString('utf8'));
  } catch (e) {
    log.error('Invalid JSON payload', { error: e?.message });
    await publishDLQ(ch, null, correlationId, 0, 'json_parse_error', msg.content);
    ch.ack(msg); // FIXED: Always ack to avoid redelivery loops
    return;
  }

  // Validate schema
  if (!validatePaymentEvent || !validatePaymentEvent(content)) {
    const errors = validatePaymentEvent ? validatePaymentEvent.errors : [{ message: 'schema_not_loaded' }];
    log.error('Validation failed for PaymentEvent', { errors });
    await publishDLQ(ch, content, correlationId, 0, 'validation_error');
    ch.ack(msg);
    return;
  }

  const attempts = Number((msg.properties.headers && msg.properties.headers.attempts) || 0);

  // Idempotency (paymentId)
  const paymentId = content.paymentId;
  const idemKey = `shipping:processed:${paymentId}`; // FIXED: Redis-backed idempotency key

  try {
    // FIXED: Idempotency via Redis SET NX EX 86400
    const setResult = await redisClient.set(idemKey, '1', { NX: true, EX: IDEMP_TTL_SECONDS });
    if (setResult !== 'OK') {
      // Duplicate
      metricDuplicate.inc(); // FIXED
      log.warn('Duplicate event ignored (idempotent)', { paymentId });
      ch.ack(msg);
      return;
    }
  } catch (e) {
    // Redis failures are recoverable; retry with backoff using retry mechanism
    log.error('Redis error during idempotency check', { error: e?.message });
    if (attempts + 1 < MAX_ATTEMPTS) {
      await publishRetry(ch, msg, correlationId, attempts);
    } else {
      await publishDLQ(ch, content, correlationId, attempts, 'redis_error');
    }
    ch.ack(msg);
    return;
  }

  // Business logic placeholder
  try {
    // FIXED: Simulate shipping scheduling
    // In a real implementation, call downstream systems here.
    log.info('Shipping scheduled', {
      paymentId: content.paymentId,
      orderId: content.orderId,
      amount: content.amount,
      currency: content.currency,
      status: content.status,
    });

    metricProcessed.inc({ status: 'success' }); // FIXED
    ch.ack(msg); // FIXED: Ack after success
  } catch (e) {
    log.error('Business processing error', { error: e?.message });

    if (attempts + 1 < MAX_ATTEMPTS) {
      await publishRetry(ch, msg, correlationId, attempts); // FIXED: bounded retries
      ch.ack(msg);
    } else {
      await publishDLQ(ch, content, correlationId, attempts, 'processing_error'); // FIXED: final DLQ
      ch.ack(msg);
    }
  }
}

async function startConsumer(ch) {
  // FIXED: Ensure channel exists before consuming and set QoS
  await ch.prefetch(10);

  await ch.consume(
    QUEUE_MAIN,
    async (msg) => {
      try {
        await processMessage(ch, msg);
      } catch (e) {
        // Safety net: always ack with DLQ publication on unexpected errors
        const cid = getCorrelationIdFromMsg(msg);
        const log = logger.child({ correlationId: cid });
        log.error('Unhandled consumer error', { error: e?.message });

        try {
          await publishDLQ(ch, null, cid, 0, 'unhandled_consumer_error', msg?.content);
        } catch (pubErr) {
          log.error('Failed to publish to DLQ on unhandled error', { error: pubErr?.message });
        } finally {
          ch.ack(msg);
        }
      }
    },
    { noAck: false },
  );

  logger.info('Consumer started', { queue: QUEUE_MAIN });
}

async function connectRabbitWithBackoff() {
  if (reconnecting) return;
  reconnecting = true;

  while (true) {
    try {
      logger.info('Connecting to RabbitMQ...', { AMQP_URL });
      amqpConnection = await amqp.connect(AMQP_URL);
      isRabbitConnected = true;
      logger.info('RabbitMQ connected');

      amqpConnection.on('error', (err) => {
        isRabbitConnected = false;
        logger.error('RabbitMQ connection error', { error: err?.message });
      });

      amqpConnection.on('close', async () => {
        isRabbitConnected = false;
        logger.warn('RabbitMQ connection closed, scheduling reconnect...');
        scheduleReconnect();
      });

      amqpChannel = await amqpConnection.createChannel();
      amqpChannel.on('error', (err) => {
        logger.error('RabbitMQ channel error', { error: err?.message });
      });
      amqpChannel.on('close', () => {
        logger.warn('RabbitMQ channel closed');
      });

      await setupTopology(amqpChannel); // FIXED: auto-declare topology
      await startConsumer(amqpChannel); // FIXED: start consuming only after channel exists

      // Reset backoff after success
      backoffMs = 1000;
      reconnecting = false;
      return; // Connected and consuming
    } catch (err) {
      isRabbitConnected = false;
      logger.error('Failed to connect/initialize RabbitMQ', { error: err?.message, backoffMs });
      await new Promise((r) => setTimeout(r, backoffMs));
      backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
      // loop continues
    }
  }
}

function scheduleReconnect() {
  if (reconnecting) return;
  setTimeout(() => {
    connectRabbitWithBackoff().catch((e) => {
      logger.error('Reconnect attempt failed', { error: e?.message });
    });
  }, Math.min(backoffMs, MAX_BACKOFF_MS));
}

// -------------------------------------------------------------------------------------
// HTTP Server (Health and Metrics)
// -------------------------------------------------------------------------------------

const app = express();
app.use(express.json());

// FIXED: CorrelationId middleware for HTTP requests
app.use((req, res, next) => {
  const correlationId = req.headers['x-correlation-id'] || req.headers['correlationid'] || randomUUID();
  req.correlationId = correlationId;
  res.setHeader('x-correlation-id', correlationId);
  next();
});

// FIXED: HTTP metrics timing middleware
app.use((req, res, next) => {
  const end = httpRequestDuration.startTimer();
  res.on('finish', () => {
    end({ method: req.method, route: req.route ? req.route.path : req.path, status_code: res.statusCode });
  });
  next();
});

// FIXED: Health endpoint reports readiness of Redis and RabbitMQ
app.get('/health', (req, res) => {
  const healthy = isRedisConnected && isRabbitConnected;
  const body = { status: healthy ? 'UP' : 'DOWN' };
  res.status(healthy ? 200 : 503).json(body);
});

// FIXED: Metrics endpoint exposes Prometheus registry
app.get('/metrics', async (req, res) => {
  try {
    res.set('Content-Type', register.contentType);
    res.end(await register.metrics());
  } catch (e) {
    res.status(500).json({ error: e?.message || 'metrics_error' });
  }
});

// Root for simple diagnostics
app.get('/', (req, res) => {
  res.json({ service: SERVICE_NAME, env: NODE_ENV });
});

// -------------------------------------------------------------------------------------
// Startup
// -------------------------------------------------------------------------------------

async function start() {
  // FIXED: Ensure Redis connects at startup
  try {
    await redisClient.connect();
  } catch (e) {
    logger.error('Redis initial connect failed', { error: e?.message });
    // Keep running; health will show DOWN until it reconnects
  }

  // FIXED: Connect RabbitMQ with exponential backoff
  connectRabbitWithBackoff().catch((e) => {
    logger.error('Initial RabbitMQ connect failed', { error: e?.message });
  });

  app.listen(PORT, () => {
    logger.info(`Shipping service listening on port ${PORT}`);
  });
}

// Graceful shutdown
async function shutdown() {
  logger.info('Shutting down...');
  try {
    if (amqpChannel) await amqpChannel.close();
  } catch { /* ignore */ }
  try {
    if (amqpConnection) await amqpConnection.close();
  } catch { /* ignore */ }
  try {
    await redisClient.quit();
  } catch { /* ignore */ }
  process.exit(0);
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

// Start service
start().catch((e) => {
  logger.error('Startup error', { error: e?.message });
});