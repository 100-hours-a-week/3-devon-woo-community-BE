package com.devon.techblog.application.member.validator;

import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthValidator {

    private final MemberRepository memberRepository;

    /**
     * 회원가입 요청 검증
     */
    public void validateSignup(SignupRequest request) {
        validateEmailNotDuplicated(request.email());
        validateNicknameNotDuplicated(request.nickname());
    }

    private void validateEmailNotDuplicated(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateNicknameNotDuplicated(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
