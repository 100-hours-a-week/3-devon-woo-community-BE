package com.kakaotechbootcamp.community.fake;

import com.kakaotechbootcamp.community.infra.redis.adapter.RedisService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FakeRedisService implements RedisService {

    private final Map<String, String> store = new ConcurrentHashMap<>();
    private final Map<String, Instant> expiry = new ConcurrentHashMap<>();

    @Override
    public void save(String key, String value, Duration ttl) {
        store.put(key, value);

        if (ttl != null) {
            expiry.put(key, Instant.now().plus(ttl));
        } else {
            expiry.remove(key);
        }
    }

    @Override
    public Optional<String> find(String key) {
        // TTL 체크
        if (isExpired(key)) {
            delete(key); // 만료된 경우 Redis처럼 자동 삭제
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        expiry.remove(key);
    }

    private boolean isExpired(String key) {
        Instant exp = expiry.get(key);
        return exp != null && Instant.now().isAfter(exp);
    }
}
