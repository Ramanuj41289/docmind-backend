
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

	private JwtService jwtService;

	private UserDetails userDetails;

	private static final String SECRET = "DocMindJwtSecretKeyForTesting12345678901234567890";

	private static final long EXPIRATION = 3600000L;

	private static final long TIMESTAMP_TOLERANCE = 1000L;

	@BeforeEach
	void setUp() {

		jwtService = new JwtService();

		ReflectionTestUtils.setField(jwtService, "secret", SECRET);

		ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);

		userDetails = User.withUsername("testuser").password("password").roles("USER").build();
	}

	@Test
	void generateToken_shouldGenerateValidJwtToken() {

		String token = jwtService.generateToken(userDetails);

		assertNotNull(token);
		assertFalse(token.isBlank());

		assertEquals(3, token.split("\\.").length);
	}

	@Test
	void generateToken_shouldContainCorrectUsername() {

		String token = jwtService.generateToken(userDetails);

		String username = jwtService.extractUsername(token);

		assertEquals("testuser", username);
	}

	@Test
	void generateToken_shouldContainIssuedAtAndExpiration() {

		long beforeGeneration = System.currentTimeMillis();

		String token = jwtService.generateToken(userDetails);

		long afterGeneration = System.currentTimeMillis();

		Claims claims = jwtService.getPayload(token);

		assertNotNull(claims.getIssuedAt());
		assertNotNull(claims.getExpiration());

		long issuedAt = claims.getIssuedAt().getTime();

		long expiration = claims.getExpiration().getTime();

		assertTrue(issuedAt >= beforeGeneration - TIMESTAMP_TOLERANCE,
				"IssuedAt should be within one second of token generation");

		assertTrue(issuedAt <= afterGeneration, "IssuedAt should not be in the future");

		assertEquals(EXPIRATION, expiration - issuedAt, "Token expiration should match configured duration");
	}

	@Test
	void getPayload_shouldReturnValidClaims() {

		String token = jwtService.generateToken(userDetails);

		Claims claims = jwtService.getPayload(token);

		assertNotNull(claims);
		assertEquals("testuser", claims.getSubject());
		assertNotNull(claims.getIssuedAt());
		assertNotNull(claims.getExpiration());
	}

	@Test
	void extractUsername_shouldReturnTokenSubject() {

		String token = jwtService.generateToken(userDetails);

		String username = jwtService.extractUsername(token);

		assertEquals(userDetails.getUsername(), username);
	}

	@Test
	void isTokenExpired_shouldReturnFalseForValidToken() {

		String token = jwtService.generateToken(userDetails);

		boolean expired = jwtService.isTokenExpired(token);

		assertFalse(expired);
	}

	@Test
	void isTokenExpired_shouldReturnTrueForExpiredToken() {

		String expiredToken = createExpiredToken();

		assertTrue(jwtService.isTokenExpired(expiredToken));
	}

	@Test
	void isTokenExpired_shouldReturnTrueWhenExpirationIsMissing() {

		String token = createTokenWithoutExpiration();

		assertTrue(jwtService.isTokenExpired(token));
	}

	@Test
	void isTokenValid_shouldReturnTrueForMatchingUsername() {

		String token = jwtService.generateToken(userDetails);

		boolean valid = jwtService.isTokenValid(token, userDetails);

		assertTrue(valid);
	}

	@Test
	void isTokenValid_shouldReturnFalseForDifferentUsername() {

		String token = jwtService.generateToken(userDetails);

		UserDetails differentUser = User.withUsername("anotheruser").password("password").roles("USER").build();

		boolean valid = jwtService.isTokenValid(token, differentUser);

		assertFalse(valid);
	}

	@Test
	void isTokenValid_shouldReturnFalseWhenExpirationIsMissing() {

		String token = createTokenWithoutExpiration();

		assertFalse(jwtService.isTokenValid(token, userDetails));
	}

	@Test
	void getIssuer_shouldReturnNullWhenIssuerIsNotConfigured() {

		String token = jwtService.generateToken(userDetails);

		String issuer = jwtService.getIssuer(token);

		assertNull(issuer);
	}

	@Test
	void getIssuer_shouldReturnConfiguredIssuer() {

		String token = Jwts.builder().subject("testuser").issuer("DocMind").issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + EXPIRATION))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();

		assertEquals("DocMind", jwtService.getIssuer(token));
	}

	@Test
	void extractUsername_shouldRejectTokenSignedWithDifferentSecret() {

		String token = jwtService.generateToken(userDetails);

		JwtService anotherJwtService = new JwtService();

		ReflectionTestUtils.setField(anotherJwtService, "secret", "DifferentSecretKeyForTesting123456789012345678");

		ReflectionTestUtils.setField(anotherJwtService, "expiration", EXPIRATION);

		assertThrows(JwtException.class, () -> anotherJwtService.extractUsername(token));
	}

	@Test
	void extractUsername_shouldRejectMalformedToken() {

		String malformedToken = "invalid.jwt.token";

		assertThrows(JwtException.class, () -> jwtService.extractUsername(malformedToken));
	}

	@Test
	void isTokenValid_shouldRejectExpiredToken() {

		String expiredToken = createExpiredToken();

		assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
	}

	private String createExpiredToken() {

		long now = System.currentTimeMillis();

		return Jwts.builder().subject("testuser").issuedAt(new Date(now - 10_000)).expiration(new Date(now - 5_000))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
	}

	private String createTokenWithoutExpiration() {

		return Jwts.builder().subject("testuser").issuedAt(new Date())
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
	}
}
