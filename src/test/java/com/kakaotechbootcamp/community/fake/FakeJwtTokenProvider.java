package com.kakaotechbootcamp.community.fake;

import com.kakaotechbootcamp.community.application.security.constants.JwtConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class FakeJwtTokenProvider {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-must-be-long-enough";
    private final SecretKey secretKey;

    public FakeJwtTokenProvider() {
        this.secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateExpiredRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() - 1000);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(JwtConstants.CLAIM_TYPE, JwtConstants.TOKEN_TYPE_REFRESH)
                .issuedAt(new Date(now.getTime() - 2000))
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateExpiredAccessToken(Long memberId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() - 1000);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(JwtConstants.CLAIM_ROLE, role)
                .claim(JwtConstants.CLAIM_TYPE, JwtConstants.TOKEN_TYPE_ACCESS)
                .issuedAt(new Date(now.getTime() - 2000))
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
}
