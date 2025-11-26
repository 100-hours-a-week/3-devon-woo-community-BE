package com.devon.techblog.application.member.service;

import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.application.member.dto.response.MemberUpdateResponse;
import com.devon.techblog.application.member.validator.MemberValidator;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.entity.MemberStatus;
import com.devon.techblog.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;

    /**
     * 회원 프로필 조회
     */
    @Transactional(readOnly = true)
    public MemberDetailsResponse getMemberProfile(Long id) {
        Member member = findMemberById(id);
        return MemberDetailsResponse.of(member);
    }

    /**
     * 회원 정보 수정
     */
    @Transactional
    public MemberUpdateResponse updateMember(Long id, MemberUpdateRequest request) {
        Member member = findMemberById(id);

        if (request.nickname() != null) {
            memberValidator.validateNicknameNotDuplicated(request.nickname(), member);
            member.changeNickname(request.nickname());
        }

        if (request.profileImage() != null) {
            member.updateProfileImage(request.profileImage());
        }

        memberRepository.save(member);

        return MemberUpdateResponse.of(member);
    }

    /**
     * 회원 비밀 번호 검증 및 변경
     */
    @Transactional
    public void updatePassword(Long id, PasswordUpdateRequest request) {
        Member member = findMemberById(id);

        memberValidator.validatePasswordUpdate(request, member);

        member.changePassword(request.newPassword());

        memberRepository.save(member);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteMember(Long id) {
        Member member = findMemberById(id);
        member.withdraw();
        memberRepository.save(member);
    }


    private Member findMemberById(Long id) {
        return memberRepository.findByIdAndStatus(id, MemberStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(MemberErrorCode.USER_NOT_FOUND));
    }
}
