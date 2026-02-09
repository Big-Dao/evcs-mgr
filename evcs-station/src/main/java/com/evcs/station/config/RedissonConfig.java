package com.evcs.station.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * Redisson配置
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class RedissonConfig {

    private final RedisProperties redisProperties;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d",
            redisProperties.getHost(),
            redisProperties.getPort());

        SingleServerConfig singleServer = config.useSingleServer()
            .setAddress(address)
            .setDatabase(redisProperties.getDatabase())
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(24)
            .setConnectTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);

        if (StringUtils.hasText(redisProperties.getPassword())) {
            singleServer.setPassword(redisProperties.getPassword());
        }

        config.setCodec(new JsonJacksonCodec());
        return Redisson.create(config);
    }
}
