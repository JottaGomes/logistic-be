package com.adrianojlt.logistics.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-256-algorithm";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtil.generateToken("adriano");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsTheUsernameEmbeddedInToken() {
        String token = jwtUtil.generateToken("adriano");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("adriano");
    }

    @Test
    void isTokenValid_withFreshToken_returnsTrue() {
        String token = jwtUtil.generateToken("adriano");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withTamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken("adriano");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        JwtUtil expiredUtil = new JwtUtil(SECRET, -1L);
        String token = expiredUtil.generateToken("adriano");
        assertThat(expiredUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_withEmptyString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_withRandomString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String tokenA = jwtUtil.generateToken("adriano");
        String tokenB = jwtUtil.generateToken("dachser");
        assertThat(tokenA).isNotEqualTo(tokenB);
        assertThat(jwtUtil.extractUsername(tokenA)).isEqualTo("adriano");
        assertThat(jwtUtil.extractUsername(tokenB)).isEqualTo("dachser");
    }
}
