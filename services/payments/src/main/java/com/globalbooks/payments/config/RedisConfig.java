package com.globalbooks.payments.config; // FIXED: New Redis configuration for idempotency keys

import org.springframework.context.annotation.Bean; // FIXED
import org.springframework.context.annotation.Configuration; // FIXED
import org.springframework.context.annotation.Primary; // FIXED: Make our RedisTemplate primary to avoid ambiguity
import org.springframework.data.redis.connection.RedisConnectionFactory; // FIXED
import org.springframework.data.redis.core.RedisTemplate; // FIXED
import org.springframework.data.redis.serializer.StringRedisSerializer; // FIXED

@Configuration // FIXED: Register Redis beans
public class RedisConfig { // FIXED

    @Bean
    @Primary // FIXED: Ensure this template is injected by default for RedisTemplate<String,String>
    public RedisTemplate<String, String> idempotencyRedisTemplate(RedisConnectionFactory connectionFactory) { // FIXED: Dedicated template bean name
        RedisTemplate<String, String> template = new RedisTemplate<>(); // FIXED
        template.setConnectionFactory(connectionFactory); // FIXED: Uses spring.redis.* properties
        StringRedisSerializer stringSerializer = new StringRedisSerializer(); // FIXED
        template.setKeySerializer(stringSerializer); // FIXED
        template.setValueSerializer(stringSerializer); // FIXED
        template.setHashKeySerializer(stringSerializer); // FIXED
        template.setHashValueSerializer(stringSerializer); // FIXED
        template.afterPropertiesSet(); // FIXED
        return template; // FIXED
    }
}