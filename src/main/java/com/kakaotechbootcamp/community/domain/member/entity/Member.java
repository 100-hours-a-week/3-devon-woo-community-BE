package com.kakaotechbootcamp.community.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.Assert;

import java.time.Instant;

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

    public static Member create(String email, String password, String nickname) {
        validateCreate(email, password, nickname);
        return Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
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


    private static void validateCreate(String email, String password, String nickname){
        Assert.hasText(email, "email required");
        Assert.hasText(password, "password required");
        Assert.hasText(nickname, "nickname required");

        if (nickname.length() > 10) {
            throw new IllegalArgumentException("nickname too long");
        }
    }
}
