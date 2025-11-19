package com.kakaotechbootcamp.community.infra.redis.adapter;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 값 저장
     */
    public void save(String key, String value, Duration ttl){
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 값 조회
     */
    public Optional<String> find(String key){
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    /**
     * 값 삭제
     */
    public void delete(String key){
        redisTemplate.delete(key);
    }

}
