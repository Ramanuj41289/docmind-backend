
package com.ramanuj.docmind.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ramanuj.docmind.service.CustomUserDetail;
import com.ramanuj.docmind.service.CustomUserDetailService;
import com.ramanuj.docmind.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtService jwtService;
	private final CustomUserDetailService userDetailService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {

			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7).trim();

		try {

			String username = jwtService.extractUsername(token);

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				CustomUserDetail userDetails = (CustomUserDetail) userDetailService.loadUserByUsername(username);

				if (jwtService.isTokenValid(token, userDetails)) {

					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails.getUser(), null, userDetails.getAuthorities());

					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}

		} catch (JwtException | IllegalArgumentException ex) {

			SecurityContextHolder.clearContext();

			logger.debug("Invalid JWT token: {}", ex.getClass().getSimpleName());

		} catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {

			SecurityContextHolder.clearContext();

			logger.debug("JWT subject no longer exists");
		}

		filterChain.doFilter(request, response);
	}
}
