package com.kakaotechbootcamp.community.application.security.dto;

import com.kakaotechbootcamp.community.domain.member.entity.MemberRole;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long uid;
    private final String password;
    private final String role;
    private final boolean isActive;

    public CustomUserDetails(Long uid, String password, MemberRole role){
        this.uid = uid;
        this.password = password;
        this.role = role.name();
        this.isActive = true;
    }

    public CustomUserDetails(Long uid, String password, String role){
        this.uid = uid;
        this.password = password;
        this.role = role;
        this.isActive = true;
    }

    public CustomUserDetails(Long uid, String password, MemberRole role, boolean isActive){
        this.uid = uid;
        this.password = password;
        this.role = role.name();
        this.isActive = isActive;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    @Override
    public String getUsername() {
        return uid.toString();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public Long getUid() {
        return uid;
    }

    public String getRole() {
        return role;
    }
}
