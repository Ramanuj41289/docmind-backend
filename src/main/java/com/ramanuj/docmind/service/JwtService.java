
package com.ramanuj.docmind.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private long expiration;

	private SecretKey getSigningKey() {

		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(UserDetails userDetails) {

		Date now = new Date();

		return Jwts.builder().subject(userDetails.getUsername()).issuedAt(now)
				.expiration(new Date(now.getTime() + expiration)).signWith(getSigningKey()).compact();
	}

	public Claims getPayload(String token) {

		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}

	public String extractUsername(String token) {

		return getPayload(token).getSubject();
	}

	public boolean isTokenExpired(String token) {

		try {
			Date expirationDate = getPayload(token).getExpiration();

			// A token without an expiration date is considered invalid.
			return expirationDate == null || !expirationDate.after(new Date());

		} catch (ExpiredJwtException exception) {

			// The signature has been verified, but the token has expired.
			return true;
		}
	}

	public String getIssuer(String token) {

		return getPayload(token).getIssuer();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {

		try {
			Claims claims = getPayload(token);

			String username = claims.getSubject();
			Date expirationDate = claims.getExpiration();

			return Objects.equals(username, userDetails.getUsername()) && expirationDate != null
					&& expirationDate.after(new Date());

		} catch (ExpiredJwtException exception) {

			return false;
		}
	}
}
