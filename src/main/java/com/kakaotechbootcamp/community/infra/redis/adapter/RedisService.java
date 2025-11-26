package com.kakaotechbootcamp.community.infra.redis.adapter;

import java.time.Duration;
import java.util.Optional;

public interface RedisService {

    void save(String key, String value,  Duration ttl);

    Optional<String> find(java.lang.String key);

    void delete(String key);

}
