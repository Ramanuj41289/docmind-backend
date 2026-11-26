package com.ramanuj.docmind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

	private final AppProperties appProperties;

	@Override
	public void addCorsMappings(CorsRegistry corsRegistry) {

		String[] allowedOrigins = appProperties.getCors().getAllowedOrigin().split(",");
		String[] allowedMethods = appProperties.getCors().getAllowedMethods().split(",");
		String[] allowedHeaders = appProperties.getCors().getAllowedHeaders().split(",");

		corsRegistry.addMapping("/**")
				.allowedOriginPatterns(allowedOrigins)
				.allowedMethods(allowedMethods)
				.allowedHeaders(allowedHeaders)
				.allowCredentials(true);

	}

}