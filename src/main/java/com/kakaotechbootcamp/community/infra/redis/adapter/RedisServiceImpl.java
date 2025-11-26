package com.kakaotechbootcamp.community.infra.redis.adapter;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String key, String value, Duration ttl){
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> find(String key){
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void delete(String key){
        redisTemplate.delete(key);
    }

}
