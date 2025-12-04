package com.devon.techblog.domain.member.entity.oauth;

import com.devon.techblog.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;


@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oauth_member")
public class OAuthMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public static OAuthMember create(String provider, String providerId, Member member){
        createValidate(provider, providerId, member);
        return OAuthMember.builder()
                .provider(provider)
                .providerId(providerId)
                .member(member)
                .build();
    }

    private static void createValidate(String provider, String providerId, Member member){
        Assert.hasText(provider, "provider required");
        Assert.hasText(providerId, "providerId required");
        Assert.notNull(member, "member required");

        // 허용되지 않은 provider인 경우 검증
    }

}
