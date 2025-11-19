package com.kakaotechbootcamp.community.infra.redis.domain;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "example", timeToLive = 7 * 24 * 60 * 60)
public class RedisExampleEntity {

    @Id
    private String key;

    private String value;

    public RedisExampleEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static RedisExampleEntity create(String key, String value) {
        return new RedisExampleEntity(key, value);
    }
}
