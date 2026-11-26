
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

class JwtServiceEdgeCaseTest {

	private static final String SECRET = "docmind-test-secret-key-0123456789abcdef";

	private static final String DIFFERENT_SECRET = "different-test-secret-key-0123456789";

	private static final long EXPIRATION = 60_000L;

	private JwtService jwtService;

	private UserDetails userDetails;

	private SecretKey signingKey;

	@BeforeEach
	void setUp() {

		jwtService = new JwtService();

		ReflectionTestUtils.setField(jwtService, "secret", SECRET);
		ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);

		signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

		userDetails = User.withUsername("testuser").password("encoded-password").authorities("ROLE_USER").build();
	}

	@Test
	void generateToken_shouldCreateTokenWithCorrectSubjectAndExpiration() {

		String token = jwtService.generateToken(userDetails);

		assertEquals("testuser", jwtService.extractUsername(token));

		Date expirationDate = jwtService.getPayload(token).getExpiration();

		assertTrue(expirationDate.after(new Date()));

		assertTrue(expirationDate.getTime() <= System.currentTimeMillis() + EXPIRATION + 2000);
	}

	@Test
	void getPayload_shouldRejectTokenSignedWithDifferentSecret() {

		SecretKey differentKey = Keys.hmacShaKeyFor(DIFFERENT_SECRET.getBytes(StandardCharsets.UTF_8));

		String token = Jwts.builder().subject("testuser").issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + EXPIRATION)).signWith(differentKey).compact();

		assertThrows(SignatureException.class, () -> jwtService.getPayload(token));
	}

	@Test
	void getPayload_shouldRejectExpiredToken() {

		String expiredToken = createExpiredToken();

		assertThrows(ExpiredJwtException.class, () -> jwtService.getPayload(expiredToken));
	}

	@Test
	void extractUsername_shouldRejectExpiredToken() {

		String expiredToken = createExpiredToken();

		assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(expiredToken));
	}

	@Test
	void isTokenExpired_shouldReturnTrueForExpiredToken() {

		String expiredToken = createExpiredToken();

		assertTrue(jwtService.isTokenExpired(expiredToken));
	}

	@Test
	void isTokenExpired_shouldReturnFalseForValidToken() {

		String token = jwtService.generateToken(userDetails);

		assertFalse(jwtService.isTokenExpired(token));
	}

	@Test
	void isTokenValid_shouldReturnFalseWhenUsernameDoesNotMatch() {

		String token = jwtService.generateToken(userDetails);

		UserDetails differentUser = User.withUsername("anotheruser").password("encoded-password")
				.authorities("ROLE_USER").build();

		assertFalse(jwtService.isTokenValid(token, differentUser));
	}

	@Test
	void isTokenValid_shouldReturnFalseForExpiredToken() {

		String expiredToken = createExpiredToken();

		assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
	}

	@Test
	void isTokenValid_shouldReturnTrueForValidToken() {

		String token = jwtService.generateToken(userDetails);

		assertTrue(jwtService.isTokenValid(token, userDetails));
	}

	@Test
	void getIssuer_shouldReturnNullWhenIssuerIsNotPresent() {

		String token = jwtService.generateToken(userDetails);

		assertNull(jwtService.getIssuer(token));
	}

	private String createExpiredToken() {

		long now = System.currentTimeMillis();

		return Jwts.builder().subject("testuser").issuedAt(new Date(now - 10_000)).expiration(new Date(now - 5_000))
				.signWith(signingKey).compact();
	}
}
