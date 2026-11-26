
package com.ramanuj.docmind.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.modelmapper.ModelMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.session.SessionService;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class ProjectConfigTest {
	private ProjectConfig projectConfig;

	@BeforeEach
	void setUp() {
		projectConfig = new ProjectConfig();
	}

	@Test
	void openapi_shouldConfigureApiMetadata() {
		OpenAPI openAPI = projectConfig.openapi();
		assertThat(openAPI).isNotNull();
		assertThat(openAPI.getInfo()).isNotNull();
		assertThat(openAPI.getInfo().getTitle()).isEqualTo("DocMind - AI Document Intelligence & RAG backend");
		assertThat(openAPI.getInfo().getDescription()).isEqualTo("REST API for document ingestion, pgvector embeddings and conversational Q&A.");
		assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
		assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Rammstein Technologies");
		assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("ramanuj.ghosh.89@gmail.com");
		assertThat(openAPI.getInfo().getContact().getUrl()).isEqualTo("https://www.linkedin.com/in/ramanuj41289/");
	}

	@Test
	void openapi_shouldConfigureJwtBearerSecurityScheme() {
		OpenAPI openAPI = projectConfig.openapi();
		assertThat(openAPI.getComponents()).isNotNull();
		assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
		SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
		assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
		assertThat(securityScheme.getScheme()).isEqualTo("bearer");
		assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
		assertThat(securityScheme.getName()).isEqualTo("bearerAuth");
	}

	@Test
	void modelMapper_shouldCreateModelMapperBean() {
		ModelMapper modelMapper = projectConfig.modelMapper();
		assertThat(modelMapper).isNotNull();
	}

	@Test
	void chatClient_shouldConfigureSystemPromptAndBuildClient() {
	    ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_SELF);
	    ChatClient chatClient = mock(ChatClient.class);
	    SessionService sessionService = mock(SessionService.class);
	    when(builder.build()).thenReturn(chatClient);
	    ChatClient result = projectConfig.chatClient(builder, sessionService);
	    assertThat(result).isNotNull();
	    assertThat(result).isSameAs(chatClient);
	    verify(builder).defaultSystem(anyString());
	    verify(builder).defaultAdvisors(ArgumentMatchers.any(Advisor[].class));
	    verify(builder).build();
	}
}
