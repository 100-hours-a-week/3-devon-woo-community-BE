package com.kakaotechbootcamp.community.application.auth.service;

import com.kakaotechbootcamp.community.application.auth.dto.LoginRequest;
import com.kakaotechbootcamp.community.application.auth.dto.LoginResponse;
import com.kakaotechbootcamp.community.application.auth.dto.SignupRequest;
import com.kakaotechbootcamp.community.application.auth.dto.SignupResponse;
import com.kakaotechbootcamp.community.application.auth.validator.AuthValidator;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.MemberErrorCode;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
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

    @Transactional
    public LoginResponse login(LoginRequest request){
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(MemberErrorCode.USER_NOT_FOUND));

        authValidator.validatePassword(request.password(), member.getPassword());

        return new LoginResponse(member.getId());
    }
}
