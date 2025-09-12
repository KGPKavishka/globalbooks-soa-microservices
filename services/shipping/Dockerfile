# FIXED: clarify build steps and env defaults; Node 18 per engines
FROM node:18-alpine

# FIXED: set working directory
WORKDIR /app

# FIXED: install curl for HEALTHCHECK
RUN apk add --no-cache curl

# FIXED: copy only package manifests first to leverage Docker layer caching
COPY package*.json ./

# FIXED: install production dependencies only (no lockfile -> install)
ENV NODE_ENV=production
RUN npm install --omit=dev && npm cache clean --force

# FIXED: copy the rest of the source
COPY . .

# FIXED: default envs for container networking (RabbitMQ/Redis service names)
ENV RABBITMQ_HOST=rabbitmq \
    RABBITMQ_PORT=5672 \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    PORT=3000

# FIXED: keep node_modules volume pattern
VOLUME ["/app/node_modules"]

# FIXED: expose service port
EXPOSE 3000

# FIXED: add healthcheck hitting the /health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -fsS http://localhost:3000/health || exit 1

# FIXED: start the service
CMD ["node", "app.js"]