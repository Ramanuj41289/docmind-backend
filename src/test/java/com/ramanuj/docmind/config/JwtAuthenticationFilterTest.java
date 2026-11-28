
package com.ramanuj.docmind.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.service.CustomUserDetail;
import com.ramanuj.docmind.service.CustomUserDetailService;
import com.ramanuj.docmind.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private CustomUserDetailService userDetailService;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	private User user;

	private CustomUserDetail userDetails;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private static final String TOKEN = "valid.jwt.token";

	@BeforeEach
	void setUp() {

		SecurityContextHolder.clearContext();

		user = User.builder().id(1L).username("testuser").password("encoded-password").email("test@example.com")
				.role(Role.USER).build();

		userDetails = new CustomUserDetail(user);

		request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/documents");

		response = new MockHttpServletResponse();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilter_shouldAuthenticateUserWithValidToken() throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenReturn("testuser");
		when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		assertNotNull(authentication);
		assertSame(user, authentication.getPrincipal());

		assertEquals(List.of(new SimpleGrantedAuthority("ROLE_USER")),
				authentication.getAuthorities().stream().toList());

		assertNotNull(authentication.getDetails());

		verify(jwtService).extractUsername(TOKEN);
		verify(userDetailService).loadUserByUsername("testuser");
		verify(jwtService).isTokenValid(TOKEN, userDetails);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldContinueWithoutAuthorizationHeader() throws ServletException, IOException {

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verifyNoInteractions(jwtService);
		verifyNoInteractions(userDetailService);
		verify(filterChain).doFilter(request, response);
		assertNotNull(response.getHeader("X-Request-Id"));
		assertEquals(response.getHeader("X-Request-Id"), request.getAttribute("requestId"));
	}

	@Test
	void doFilter_shouldPropagateIncomingRequestId() throws ServletException, IOException {
		request.addHeader("X-Request-Id", "req-123");
		jwtAuthenticationFilter.doFilter(request, response, filterChain);
		assertEquals("req-123", response.getHeader("X-Request-Id"));
		assertEquals("req-123", request.getAttribute("requestId"));
	}

	@Test
	void doFilter_shouldGenerateRequestIdWhenIncomingIsBlank() throws ServletException, IOException {
		request.addHeader("X-Request-Id", "   ");
		jwtAuthenticationFilter.doFilter(request, response, filterChain);
		String generated = response.getHeader("X-Request-Id");
		assertNotNull(generated);
		assertFalse(generated.isBlank());
		assertEquals(generated, request.getAttribute("requestId"));
	}

	@Test
	void doFilter_shouldSkipAuthenticationForNonBearerHeader() throws ServletException, IOException {

		request.addHeader("Authorization", "Basic dGVzdDp0ZXN0");

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verifyNoInteractions(jwtService);
		verifyNoInteractions(userDetailService);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldClearContextWhenJwtIsInvalid() throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenThrow(new JwtException("Invalid token"));

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtService).extractUsername(TOKEN);
		verifyNoInteractions(userDetailService);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldNotAuthenticateWhenTokenValidationFails() throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenReturn("testuser");
		when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtService).extractUsername(TOKEN);
		verify(userDetailService).loadUserByUsername("testuser");
		verify(jwtService).isTokenValid(TOKEN, userDetails);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldClearContextWhenUserNoLongerExists() throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenReturn("deleteduser");

		when(userDetailService.loadUserByUsername("deleteduser"))
				.thenThrow(new UsernameNotFoundException("Invalid username or password"));

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtService).extractUsername(TOKEN);
		verify(userDetailService).loadUserByUsername("deleteduser");
		verify(jwtService, never()).isTokenValid(TOKEN, userDetails);

		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldNotOverwriteExistingAuthentication() throws ServletException, IOException {

		Authentication existingAuthentication = new UsernamePasswordAuthenticationToken("existing-user", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

		SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenReturn("testuser");

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();

		assertSame(existingAuthentication, actualAuthentication);

		verify(jwtService).extractUsername(TOKEN);
		verifyNoInteractions(userDetailService);
		verify(jwtService, never()).isTokenValid(TOKEN, userDetails);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldSkipUserLookupWhenTokenUsernameIsNull() throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenReturn(null);

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtService).extractUsername(TOKEN);
		verifyNoInteractions(userDetailService);
		verify(jwtService, never()).isTokenValid(TOKEN, userDetails);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldClearContextWhenTokenExtractionThrowsIllegalArgumentException()
			throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenThrow(new IllegalArgumentException("Invalid token argument"));

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtService).extractUsername(TOKEN);
		verifyNoInteractions(userDetailService);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldTrimWhitespaceFromBearerToken() throws ServletException, IOException {

		request.addHeader("Authorization", "Bearer   " + TOKEN + "   ");

		when(jwtService.extractUsername(TOKEN)).thenReturn("testuser");
		when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);
		when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		assertNotNull(authentication);
		assertSame(user, authentication.getPrincipal());

		verify(jwtService).extractUsername(TOKEN);
		verify(userDetailService).loadUserByUsername("testuser");
		verify(jwtService).isTokenValid(TOKEN, userDetails);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilter_shouldClearExistingAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {

		Authentication existingAuthentication = new UsernamePasswordAuthenticationToken("existing-user", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

		SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

		request.addHeader("Authorization", "Bearer " + TOKEN);

		when(jwtService.extractUsername(TOKEN)).thenThrow(new JwtException("Invalid token"));

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(jwtService).extractUsername(TOKEN);
		verifyNoInteractions(userDetailService);
		verify(filterChain).doFilter(request, response);
	}
}
