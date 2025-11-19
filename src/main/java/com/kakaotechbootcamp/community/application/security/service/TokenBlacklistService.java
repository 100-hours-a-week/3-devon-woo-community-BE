package com.kakaotechbootcamp.community.application.security.service;

import com.kakaotechbootcamp.community.application.security.util.JwtTokenProvider;
import com.kakaotechbootcamp.community.infra.redis.adapter.RedisService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:refresh-token:";
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;

    public void addToBlacklist(String token) {
        try {
            long ttl = jwtTokenProvider.getExpiresIn(token);
            String key = generateRedisKey(token);
            redisService.save(key, "0", Duration.ofMillis(ttl));
        } catch (Exception e) {
            // todo : 블랙 리스트 등록 실패시 전략 생각해보기
            log.error("블랙 리스트 등록 실패", e);
        }
    }

    public boolean isBlacklisted(String token) {
        String key = generateRedisKey(token);
        return redisService.find(key).isPresent();
    }

    private String generateRedisKey(String token){
        return BLACKLIST_PREFIX + DigestUtils.sha256Hex(token);
    }
}
