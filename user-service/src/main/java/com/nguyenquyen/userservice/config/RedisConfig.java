package com.nguyenquyen.userservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Configuration — Profile Caching
 *
 * CONCEPT: Spring Cache + Redis
 * ──────────────────────────────
 * Spring Cache abstraction (@Cacheable, @CacheEvict) works with any
 * cache backend. We configure Redis as the backend here.
 *
 * @Cacheable("profiles") on getProfileById():
 *   - First call → hit DB → store in Redis with key "profiles::userId"
 *   - Subsequent calls → return from Redis (< 1ms)
 *   - TTL: 30 minutes → auto-expire stale entries
 *
 * @CacheEvict("profiles") on updateProfile():
 *   - After update → delete from Redis → next read fetches fresh from DB
 *
 * CONCEPT: Serialization for Redis
 * ──────────────────────────────────
 * Redis stores bytes. We need to serialize Java objects.
 * GenericJackson2JsonRedisSerializer:
 *   - Stores JSON with @class field for type info
 *   - Readable by humans (good for debugging: redis-cli get "profiles::uuid")
 *   - LocalDate/LocalDateTime need JavaTimeModule to serialize correctly
 */
@Configuration
// Skip this config in tests where spring.cache.type=none (uses NoOpCacheManager instead)
// matchIfMissing=true → loads by default when property is absent (production)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper with Java 8 time support + type info for deserialization
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))           // 30 min TTL
                .disableCachingNullValues()                 // Don't cache null (404s)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
