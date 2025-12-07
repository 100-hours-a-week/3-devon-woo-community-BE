package com.devon.techblog.domain.post.policy;

import com.devon.techblog.application.post.dto.ViewContext;
import com.devon.techblog.infra.redis.adapter.RedisService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ViewCountPolicy {

    private static final Duration VIEW_COUNT_TTL = Duration.ofMinutes(10);
    private static final String VIEW_KEY_PREFIX = "view-count:";

    private final RedisService redisService;

    public boolean shouldCount(Long postId, ViewContext context){
        String redisKey = buildViewKey(postId, context);

        if (redisService.find(redisKey).isPresent()) {
            return false;
        }

        redisService.save(redisKey, "1", VIEW_COUNT_TTL);
        return true;
    }

    private String buildViewKey(Long postId, ViewContext context) {
        if (context.getMemberId() != null) {
            return VIEW_KEY_PREFIX + postId + ":member:" + context.getMemberId();
        }
        String ip = context.getIpAddress();
        String userAgent = context.getUserAgent();
        if (userAgent == null || userAgent.isBlank()) {
            return VIEW_KEY_PREFIX + postId + ":ip:" + (ip == null ? "unknown" : ip);
        }
        return VIEW_KEY_PREFIX + postId + ":ip:" + (ip == null ? "unknown" : ip) + ":ua:" + userAgent.hashCode();
    }
}
