package com.kakaotechbootcamp.community.application.member.service;

import com.kakaotechbootcamp.community.application.member.dto.request.SignupRequest;
import com.kakaotechbootcamp.community.application.member.dto.response.SignupResponse;
import com.kakaotechbootcamp.community.application.member.validator.AuthValidator;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {
    private final MemberRepository memberRepository;
    private final AuthValidator authValidator;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request){
        authValidator.validateSignup(request);

        String email = request.email();
        String password = passwordEncoder.encode(request.password());
        String nickname = request.nickname();

        Member member = Member.create(email, password, nickname);
        member.updateProfileImage(request.profileImage());

        Member savedMember = memberRepository.save(member);
        return new SignupResponse(savedMember.getId());
    }
}
