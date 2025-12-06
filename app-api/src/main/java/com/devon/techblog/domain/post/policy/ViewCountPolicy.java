package com.devon.techblog.domain.post.policy;

import com.devon.techblog.application.post.dto.ViewContext;
import org.springframework.stereotype.Component;

@Component
public class ViewCountPolicy {

    public boolean shouldCount(Long postId, ViewContext context){
        // 조회수 증가 정책, 원래는 봇 정책이나 ip검사 등으로 증가가 가능한지 판단한다.
        // 현재는 그냥 통과 시키기
        return true;
    }
}
