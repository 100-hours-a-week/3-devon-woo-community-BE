package com.kakaotechbootcamp.community.domain.member.repository;

import com.kakaotechbootcamp.community.domain.member.entity.oauth.OAuthMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthMemberRepository extends JpaRepository<OAuthMember, Long> {
    Optional<OAuthMember> findByProviderAndProviderId(String provider, String providerId);
}
