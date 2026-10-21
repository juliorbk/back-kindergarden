package com.ueclca.backend.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import com.ueclca.backend.config.JwtProperties;

class JwtConfigTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void decoderAcceptsTokenWithExpectedIssuer() {
        JwtProperties properties = new JwtProperties(SECRET, "back-kindergarten-test", 60);
        JwtDecoder decoder = jwtConfig.jwtDecoder(properties);

        String token = issueToken(jwtConfig.jwtEncoder(properties), "back-kindergarten-test");

        assertNotNull(decoder.decode(token));
    }

    @Test
    void decoderRejectsTokenFromAnotherIssuer() {
        JwtProperties encoderProperties = new JwtProperties(SECRET, "issuer-desconocido", 60);
        JwtProperties decoderProperties = new JwtProperties(SECRET, "back-kindergarten-test", 60);
        JwtDecoder decoder = jwtConfig.jwtDecoder(decoderProperties);

        String token = issueToken(jwtConfig.jwtEncoder(encoderProperties), "issuer-desconocido");

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    private String issueToken(JwtEncoder encoder, String issuer) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .subject("usuario@ueclca.edu.ve")
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
