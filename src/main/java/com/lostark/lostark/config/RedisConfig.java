package com.lostark.lostark.config;

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
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 설정: JSON 직렬화 사용
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(10)); // 기본 만료 시간은 10분으로 설정

        // 특정 캐시에 대해 만료 설정 (캐릭터 정보는 10분 정도로 유지)
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("characterCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("expeditionCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("profileCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("marketCache", defaultConfig.entryTtl(Duration.ofMinutes(1))); // 마켓은 변동이 잦으므로 짧게 유지
        cacheConfigurations.put("gemCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("auctionCache", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
