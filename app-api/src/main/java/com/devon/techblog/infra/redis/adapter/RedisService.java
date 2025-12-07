package com.devon.techblog.infra.redis.adapter;

import java.time.Duration;
import java.util.Optional;

public interface RedisService {

    void save(String key, String value,  Duration ttl);

    Optional<String> find(String key);

    void delete(String key);

}
