package com.devon.techblog.application.security.service;

import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        Long memberId = jwtTokenProvider.getUidFromToken(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.USER_NOT_FOUND));

        if (!member.isActive()) {
            throw new CustomException(MemberErrorCode.MEMBER_INACTIVE);
        }

        return jwtTokenProvider.generateAccessToken(memberId, member.getRole().name());
    }
}
