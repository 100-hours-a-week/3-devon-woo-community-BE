package com.devon.techblog.domain.member.entity;

import com.devon.techblog.application.member.dto.SocialLinks;
import com.devon.techblog.domain.common.converter.SocialLinksConverter;
import com.devon.techblog.domain.common.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", length = 10, nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "handle", length = 50)
    private String handle;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "company", length = 100)
    private String company;

    @Column(name = "location", length = 100)
    private String location;

    @Convert(converter = StringListConverter.class)
    @Column(name = "primary_stack")
    private List<String> primaryStack;

    @Convert(converter = StringListConverter.class)
    @Column(name = "interests")
    private List<String> interests;

    @Convert(converter = SocialLinksConverter.class)
    @Column(name = "social_links")
    private SocialLinks socialLinks;

    public static Member create(String email, String password, String nickname) {
        validateCreate(email, password, nickname);
        return Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .primaryStack(Collections.emptyList())
                .interests(Collections.emptyList())
                .socialLinks(SocialLinks.empty())
                .build();
    }

    public void changeNickname(String nickname) {
        Assert.hasText(nickname, "nickname required");
        if (nickname.length() > 10) {
            throw new IllegalArgumentException("nickname too long");
        }
        this.nickname = nickname;
    }

    public void updateProfileImage(String url) {
        if (url != null && url.length() > 500) {
            throw new IllegalArgumentException("url too long");
        }
        this.profileImageUrl = url;
    }

    public void changePassword(String password) {
        Assert.hasText(password, "password required");
        this.password = password;
    }

    public void loginSuccess() {
        this.lastLoginAt = Instant.now();
    }

    public void deactivate() { this.status = MemberStatus.INACTIVE; }

    public void withdraw() { this.status = MemberStatus.WITHDRAWN; }

    public boolean isActive() { return this.status == MemberStatus.ACTIVE; }

    public void updateHandle(String handle) {
        if (handle != null && handle.length() > 50) {
            throw new IllegalArgumentException("handle too long");
        }
        this.handle = handle;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updateCompany(String company) {
        if (company != null && company.length() > 100) {
            throw new IllegalArgumentException("company too long");
        }
        this.company = company;
    }

    public void updateLocation(String location) {
        if (location != null && location.length() > 100) {
            throw new IllegalArgumentException("location too long");
        }
        this.location = location;
    }

    public void updatePrimaryStack(List<String> primaryStack) {
        this.primaryStack = primaryStack != null ? primaryStack : Collections.emptyList();
    }

    public void updateInterests(List<String> interests) {
        this.interests = interests != null ? interests : Collections.emptyList();
    }

    public void updateSocialLinks(SocialLinks socialLinks) {
        this.socialLinks = socialLinks != null ? socialLinks : SocialLinks.empty();
    }

    public void updateProfile(String handle, String bio, String company, String location,
                              List<String> primaryStack, List<String> interests, SocialLinks socialLinks) {
        updateHandle(handle);
        updateBio(bio);
        updateCompany(company);
        updateLocation(location);
        updatePrimaryStack(primaryStack);
        updateInterests(interests);
        updateSocialLinks(socialLinks);
    }

    private static void validateCreate(String email, String password, String nickname){
        Assert.hasText(email, "email required");
        Assert.hasText(password, "password required");
        Assert.hasText(nickname, "nickname required");

        if (nickname.length() > 10) {
            throw new IllegalArgumentException("nickname too long");
        }
    }
}
