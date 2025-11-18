package com.kakaotechbootcamp.community.application.member.validator;

import com.kakaotechbootcamp.community.application.member.dto.request.SignupRequest;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.MemberErrorCode;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
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
            throw new CustomException(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateNicknameNotDuplicated(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new CustomException(MemberErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
