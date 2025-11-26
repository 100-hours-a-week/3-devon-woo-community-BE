package com.devon.techblog.application.security.service;

import com.devon.techblog.application.security.dto.oauth.OAuthUserInfo;
import com.devon.techblog.application.security.dto.oauth.OAuthUserInfoFactory;
import com.devon.techblog.application.security.dto.user.CustomOAuthUserDetails;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.entity.oauth.OAuthMember;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.member.repository.OAuthMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final OAuthMemberRepository oAuthMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuthUserInfo userInfo = OAuthUserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        OAuthMember oAuthMember = oAuthMemberRepository
                .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getId())
                .orElseGet(() -> registerUser(userInfo));

        Member member = oAuthMember.getMember();
        member.loginSuccess();

        return new CustomOAuthUserDetails(member.getId(), member.getRole().name());
    }

    private OAuthMember registerUser(OAuthUserInfo userInfo) {
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String randomPassword = passwordEncoder.encode("password");

        Member member = Member.create(email, randomPassword, name);
        memberRepository.save(member);

        OAuthMember oAuthMember = OAuthMember.create(userInfo.getProvider(), userInfo.getId(), member);
        return oAuthMemberRepository.save(oAuthMember);
    }
}
