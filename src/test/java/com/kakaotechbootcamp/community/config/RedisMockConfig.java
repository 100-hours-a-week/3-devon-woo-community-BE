package com.kakaotechbootcamp.community.config;

import com.kakaotechbootcamp.community.fake.FakeRedisService;
import com.kakaotechbootcamp.community.infra.redis.adapter.RedisService;
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
