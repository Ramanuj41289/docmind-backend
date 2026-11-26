
package com.ramanuj.docmind.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class CorsConfigTest {

	@Test
	void addCorsMappings_shouldRegisterDefaultCorsConfiguration() {

		AppProperties properties = new AppProperties();

		CorsRegistry registry = mock(CorsRegistry.class);
		CorsRegistration registration = mock(CorsRegistration.class);

		when(registry.addMapping("/**")).thenReturn(registration);
		when(registration.allowedOriginPatterns("*")).thenReturn(registration);
		when(registration.allowedMethods("GET", " POST", " PUT", " DELETE", " OPTIONS")).thenReturn(registration);
		when(registration.allowedHeaders("*")).thenReturn(registration);
		when(registration.allowCredentials(true)).thenReturn(registration);

		CorsConfig corsConfig = new CorsConfig(properties);

		corsConfig.addCorsMappings(registry);

		verify(registry).addMapping("/**");
		verify(registration).allowedOriginPatterns("*");
		verify(registration).allowedMethods("GET", " POST", " PUT", " DELETE", " OPTIONS");
		verify(registration).allowedHeaders("*");
		verify(registration).allowCredentials(true);
	}

	@Test
	void addCorsMappings_shouldRegisterCustomCorsConfiguration() {

		AppProperties properties = new AppProperties();

		properties.getCors().setAllowedOrigin("https://localhost:3000,https://docmind.example.com");
		properties.getCors().setAllowedMethods("GET,POST,PATCH");
		properties.getCors().setAllowedHeaders("Authorization,Content-Type,X-Request-ID");

		CorsRegistry registry = mock(CorsRegistry.class);
		CorsRegistration registration = mock(CorsRegistration.class);

		when(registry.addMapping("/**")).thenReturn(registration);
		when(registration.allowedOriginPatterns("https://localhost:3000", "https://docmind.example.com"))
				.thenReturn(registration);
		when(registration.allowedMethods("GET", "POST", "PATCH")).thenReturn(registration);
		when(registration.allowedHeaders("Authorization", "Content-Type", "X-Request-ID")).thenReturn(registration);
		when(registration.allowCredentials(true)).thenReturn(registration);

		CorsConfig corsConfig = new CorsConfig(properties);

		corsConfig.addCorsMappings(registry);

		verify(registry).addMapping("/**");
		verify(registration).allowedOriginPatterns("https://localhost:3000", "https://docmind.example.com");
		verify(registration).allowedMethods("GET", "POST", "PATCH");
		verify(registration).allowedHeaders("Authorization", "Content-Type", "X-Request-ID");
		verify(registration).allowCredentials(true);
	}

	@Test
	void addCorsMappings_shouldPreserveWhitespaceInConfiguredValues() {

		AppProperties properties = new AppProperties();

		properties.getCors().setAllowedOrigin("https://localhost:3000, https://docmind.example.com");
		properties.getCors().setAllowedMethods("GET, POST, PATCH");
		properties.getCors().setAllowedHeaders("Authorization, Content-Type");

		CorsRegistry registry = mock(CorsRegistry.class);
		CorsRegistration registration = mock(CorsRegistration.class);

		when(registry.addMapping("/**")).thenReturn(registration);

		when(registration.allowedOriginPatterns("https://localhost:3000", " https://docmind.example.com"))
				.thenReturn(registration);

		when(registration.allowedMethods("GET", " POST", " PATCH")).thenReturn(registration);

		when(registration.allowedHeaders("Authorization", " Content-Type")).thenReturn(registration);

		when(registration.allowCredentials(true)).thenReturn(registration);

		CorsConfig corsConfig = new CorsConfig(properties);

		corsConfig.addCorsMappings(registry);

		verify(registration).allowedOriginPatterns("https://localhost:3000", " https://docmind.example.com");
		verify(registration).allowedMethods("GET", " POST", " PATCH");
		verify(registration).allowedHeaders("Authorization", " Content-Type");
		verify(registration).allowCredentials(true);
	}
}
