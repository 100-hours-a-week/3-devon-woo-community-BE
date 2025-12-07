package com.devon.techblog.application.member.validator;

import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 관련 입력값 검증
 */
@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberRepository memberRepository;

    /**
     * 닉네임 중복 검증
     */
    public void validateNicknameNotDuplicated(String nickname, Member currentMember) {
        if (nickname != null && !nickname.equals(currentMember.getNickname())) {
            if (memberRepository.existsByNickname(nickname)) {
                throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
            }
        }
    }

    /**
     * 비밀번호 변경 요청 검증
     */
    public void validatePasswordUpdate(PasswordUpdateRequest request, Member member) {
        validateCurrentPassword(request.currentPassword(), member.getPassword());
        validateNewPasswordDifferent(request.newPassword(), member.getPassword());
    }

    private void validateCurrentPassword(String inputPassword, String storedPassword) {
        if (!inputPassword.equals(storedPassword)) {
            throw new BusinessException(MemberErrorCode.INVALID_CURRENT_PASSWORD);
        }
    }

    private void validateNewPasswordDifferent(String newPassword, String currentPassword) {
        if (newPassword.equals(currentPassword)) {
            throw new BusinessException(MemberErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
    }
}
