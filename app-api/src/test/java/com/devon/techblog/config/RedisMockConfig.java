package com.devon.techblog.config;

import com.devon.techblog.fake.FakeRedisService;
import com.devon.techblog.infra.redis.adapter.RedisService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RedisMockConfig {

    @Bean
    @Primary
    public RedisService testRedisService(){
        return new FakeRedisService();
    }

}
