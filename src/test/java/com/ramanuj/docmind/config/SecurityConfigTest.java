
package com.ramanuj.docmind.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.service.CustomUserDetail;
import com.ramanuj.docmind.service.CustomUserDetailService;
import com.ramanuj.docmind.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = { "jwt.secret=docmind-test-secret-key-for-jwt-testing-123456",
		"jwt.expiration=3600000" })
class SecurityConfigTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private FilterChainProxy springSecurityFilterChain;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private CustomUserDetailService userDetailService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private CorsConfigurationSource corsConfigurationSource;

	private MockMvc mockMvc;

	private User regularUser;

	private User adminUser;

	@BeforeEach
	void setUp() {

		reset(jwtService, userDetailService);

		mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();

		regularUser = User.builder().id(1L).username("testuser").email("testuser@example.com")
				.password("encodedPassword").role(Role.USER).build();

		adminUser = User.builder().id(2L).username("admin").email("admin@example.com").password("encodedPassword")
				.role(Role.ADMIN).build();
	}

	private void configureValidToken(String token, User user) {

		CustomUserDetail userDetails = new CustomUserDetail(user);

		when(jwtService.extractUsername(token)).thenReturn(user.getUsername());

		when(userDetailService.loadUserByUsername(user.getUsername())).thenReturn(userDetails);

		when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
	}

	@Test
	void publicAuthEndpoint_shouldAllowAccessWithoutToken() throws Exception {

		mockMvc.perform(get("/api/v1/auth/test")).andExpect(status().isOk())
				.andExpect(content().string("Public endpoint"));
	}

	@Test
	void swaggerAndOpenApiEndpoints_shouldAllowAccessWithoutToken() throws Exception {

		mockMvc.perform(get("/swagger-ui/test")).andExpect(status().isOk())
				.andExpect(content().string("Swagger endpoint"));

		mockMvc.perform(get("/v3/api-docs/test")).andExpect(status().isOk())
				.andExpect(content().string("OpenAPI endpoint"));
	}

	@Test
	void protectedEndpoint_shouldReturnUnauthorizedWithoutToken() throws Exception {

		mockMvc.perform(get("/api/v1/documents/test")).andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Authentication is required")));
	}

	@Test
	void protectedEndpoint_shouldAllowAccessWithValidUserToken() throws Exception {

		configureValidToken("valid-user-token", regularUser);

		mockMvc.perform(get("/api/v1/documents/test").header("Authorization", "Bearer valid-user-token"))
				.andExpect(status().isOk()).andExpect(content().string("Protected endpoint"));
	}

	@Test
	void adminEndpoint_shouldAllowAccessWithAdminToken() throws Exception {

		configureValidToken("valid-admin-token", adminUser);

		mockMvc.perform(get("/api/v1/admin/test").header("Authorization", "Bearer valid-admin-token"))
				.andExpect(status().isOk()).andExpect(content().string("Admin endpoint"));
	}

	@Test
	void adminEndpoint_shouldForbidAccessWithRegularUserToken() throws Exception {

		configureValidToken("valid-user-token", regularUser);

		mockMvc.perform(get("/api/v1/admin/test").header("Authorization", "Bearer valid-user-token"))
				.andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(content().string(
						org.hamcrest.Matchers.containsString("You do not have permission to access this resource")));
	}

	@Test
	void protectedEndpoint_shouldRejectInvalidJwtToken() throws Exception {

		when(jwtService.extractUsername("invalid-token")).thenThrow(new JwtException("Invalid token"));

		mockMvc.perform(get("/api/v1/documents/test").header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Authentication is required")));
	}

	@Test
	void protectedEndpoint_shouldRejectTokenForDeletedUser() throws Exception {

		when(jwtService.extractUsername("deleted-user-token")).thenReturn("deleteduser");

		when(userDetailService.loadUserByUsername("deleteduser"))
				.thenThrow(new UsernameNotFoundException("Invalid username or password"));

		mockMvc.perform(get("/api/v1/documents/test").header("Authorization", "Bearer deleted-user-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void optionsRequest_shouldAllowCorsPreflight() throws Exception {

		mockMvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/documents/test")
						.header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk()).andExpect(header().exists("Access-Control-Allow-Origin"));
	}

	@Test
	void passwordEncoder_shouldEncodeAndVerifyPassword() {

		String rawPassword = "Password123";

		String encodedPassword = passwordEncoder.encode(rawPassword);

		assertNotEquals(rawPassword, encodedPassword);
		assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
		assertFalse(passwordEncoder.matches("WrongPassword", encodedPassword));
	}

	@Test
	void nonApiEndpoint_shouldRequireAuthentication() throws Exception {

		mockMvc.perform(get("/outside/test")).andExpect(status().isUnauthorized());

		configureValidToken("valid-outside-token", regularUser);

		mockMvc.perform(get("/outside/test").header("Authorization", "Bearer valid-outside-token"))
				.andExpect(status().isOk()).andExpect(content().string("Outside endpoint"));
	}

	@Test
	void corsConfigurationSource_shouldApplyConfiguredCorsSettings() {

		HttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET",
				"/api/v1/documents/test");

		CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

		assertTrue(configuration.getAllowedOriginPatterns().contains("*"));
		assertTrue(configuration.getAllowedMethods().contains("GET"));
		assertTrue(configuration.getAllowedMethods().contains("POST"));
		assertTrue(configuration.getAllowedHeaders().contains("*"));
		assertTrue(configuration.getAllowCredentials());
	}

	@Test
	void corsConfigurationSource_shouldHandleNullBlankAndEmptyValues() {

		AppProperties properties = new AppProperties();

		properties.getCors().setAllowedOrigin(null);
		properties.getCors().setAllowedMethods(" ");
		properties.getCors().setAllowedHeaders(" , ");

		SecurityConfig securityConfig = new SecurityConfig(Mockito.mock(JwtAuthenticationFilter.class), properties);

		CorsConfigurationSource source = securityConfig.corsConfigurationSource();

		HttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/test");

		CorsConfiguration configuration = source.getCorsConfiguration(request);

		assertTrue(configuration.getAllowedOriginPatterns().isEmpty());
		assertTrue(configuration.getAllowedMethods().isEmpty());
		assertTrue(configuration.getAllowedHeaders().isEmpty());
		assertTrue(configuration.getAllowCredentials());
	}

	@Configuration
	@EnableWebMvc
	@Import(SecurityConfig.class)
	static class TestConfig {

		@Bean
		AppProperties appProperties() {
			return new AppProperties();
		}

		@Bean
		JwtService jwtService() {
			return Mockito.mock(JwtService.class);
		}

		@Bean
		CustomUserDetailService userDetailService() {
			return Mockito.mock(CustomUserDetailService.class);
		}

		@Bean
		JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
				CustomUserDetailService userDetailService) {
			return new JwtAuthenticationFilter(jwtService, userDetailService);
		}

		@Bean
		TestEndpoints testEndpoints() {
			return new TestEndpoints();
		}
	}

	@RestController
	static class TestEndpoints {

		@GetMapping("/api/v1/auth/test")
		String publicEndpoint() {
			return "Public endpoint";
		}

		@GetMapping("/api/v1/documents/test")
		String protectedEndpoint() {
			return "Protected endpoint";
		}

		@GetMapping("/api/v1/admin/test")
		String adminEndpoint() {
			return "Admin endpoint";
		}

		@GetMapping("/swagger-ui/test")
		String swaggerEndpoint() {
			return "Swagger endpoint";
		}

		@GetMapping("/v3/api-docs/test")
		String openApiEndpoint() {
			return "OpenAPI endpoint";
		}

		@GetMapping("/outside/test")
		String outsideEndpoint() {
			return "Outside endpoint";
		}
	}
}
