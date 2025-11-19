package com.kakaotechbootcamp.community.application.security.util;

import com.kakaotechbootcamp.community.application.security.config.JwtProperties;
import com.kakaotechbootcamp.community.application.security.constants.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties){
        this.jwtProperties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long memberId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(JwtConstants.CLAIM_ROLE, role)
                .claim(JwtConstants.CLAIM_TYPE, JwtConstants.TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(JwtConstants.CLAIM_TYPE, JwtConstants.TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getMemberIdFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get(JwtConstants.CLAIM_EMAIL, String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get(JwtConstants.CLAIM_ROLE, String.class);
    }

    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims.get(JwtConstants.CLAIM_TYPE, String.class);
    }

    public boolean isAccessToken(String token) {
        return JwtConstants.TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return JwtConstants.TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }


}
