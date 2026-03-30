package yt.wer.efms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void shouldGenerateAndValidateTokenAndExtractUsername() {
        JwtUtil jwtUtil = new JwtUtil("this-is-a-very-long-secret-for-jwt-tests-123456", 60_000L);

        String token = jwtUtil.generateToken("arnaud");

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("arnaud", jwtUtil.extractUsername(token));
    }

    @Test
    void shouldReturnFalseAndNullForInvalidToken() {
        JwtUtil jwtUtil = new JwtUtil("another-very-long-secret-for-jwt-tests-654321", 60_000L);

        assertFalse(jwtUtil.validateToken("not-a-jwt-token"));
        assertNull(jwtUtil.extractUsername("not-a-jwt-token"));
    }

    @Test
    void shouldAcceptShortSecretByDerivingStrongKey() {
        JwtUtil jwtUtil = new JwtUtil("short", 60_000L);

        String token = jwtUtil.generateToken("bob");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("bob", jwtUtil.extractUsername(token));
    }

    @Test
    void shouldRejectTokenFromDifferentSecret() {
        JwtUtil issuer = new JwtUtil("secret-one-with-enough-length-1234567890", 60_000L);
        JwtUtil validator = new JwtUtil("secret-two-with-enough-length-0987654321", 60_000L);

        String token = issuer.generateToken("arnaud");

        assertFalse(validator.validateToken(token));
        assertNull(validator.extractUsername(token));
    }
}
