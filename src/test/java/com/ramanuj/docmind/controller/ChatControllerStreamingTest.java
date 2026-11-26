
package com.ramanuj.docmind.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.ramanuj.docmind.dto.ChatRequestDto;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.service.ConversationService;
import com.ramanuj.docmind.service.RagService;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatControllerStreamingTest {

	@Mock
	private RagService ragService;

	@Mock
	private ConversationService conversationService;

	@InjectMocks
	private ChatController chatController;

	private User user;
	private Authentication authentication;

	@BeforeEach
	void setUp() {
		user = User.builder().id(1L).username("testuser").email("test@example.com").password("password").build();

		authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
	}

	@Test
	void streamQuestion_shouldReturnFluxWithExistingConversationId() {

		String conversationId = "conversation-123";

		ChatRequestDto request = ChatRequestDto.builder().question("Explain microservices")
				.conversationId(conversationId).build();

		Flux<String> expectedStream = Flux.just("Microservices ", "are independently ", "deployable services.");

		when(ragService.streamQuestionAnswer(request, user)).thenReturn(expectedStream);

		var response = chatController.streamQuestion(request, authentication);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());

		assertEquals(conversationId, response.getHeaders().getFirst("X-Conversation-Id"));

		assertNotNull(response.getBody());

		List<String> chunks = response.getBody().collectList().block();

		assertNotNull(chunks);
		assertEquals(3, chunks.size());
		assertEquals("Microservices are independently deployable services.", String.join("", chunks));

		verify(ragService).streamQuestionAnswer(request, user);
	}

	@Test
	void streamQuestion_shouldGenerateConversationIdWhenIdIsNull() {

		ChatRequestDto request = ChatRequestDto.builder().question("What is RAG?").conversationId(null).build();

		when(ragService.streamQuestionAnswer(request, user)).thenReturn(Flux.just("RAG ", "explained"));

		var response = chatController.streamQuestion(request, authentication);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		String generatedConversationId = response.getHeaders().getFirst("X-Conversation-Id");

		assertNotNull(generatedConversationId);
		assertTrue(isValidUuid(generatedConversationId));

		assertEquals(generatedConversationId, request.getConversationId());

		assertNotNull(response.getBody());

		List<String> chunks = response.getBody().collectList().block();

		assertEquals(List.of("RAG ", "explained"), chunks);

		verify(ragService).streamQuestionAnswer(request, user);
	}

	@Test
	void streamQuestion_shouldGenerateConversationIdWhenIdIsBlank() {

		ChatRequestDto request = ChatRequestDto.builder().question("Explain vector databases").conversationId("   ")
				.build();

		when(ragService.streamQuestionAnswer(request, user)).thenReturn(Flux.just("Vector ", "database"));

		var response = chatController.streamQuestion(request, authentication);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		String generatedConversationId = response.getHeaders().getFirst("X-Conversation-Id");

		assertNotNull(generatedConversationId);
		assertTrue(isValidUuid(generatedConversationId));

		assertEquals(generatedConversationId, request.getConversationId());

		assertNotNull(response.getBody());

		List<String> chunks = response.getBody().collectList().block();

		assertEquals(List.of("Vector ", "database"), chunks);

		verify(ragService).streamQuestionAnswer(request, user);
	}

	@Test
	void streamQuestion_shouldRejectRequestWithoutAuthentication() {

		ChatRequestDto request = ChatRequestDto.builder().question("Explain Spring AI")
				.conversationId("conversation-123").build();

		assertThrows(AccessDeniedException.class, () -> chatController.streamQuestion(request, null));

		verifyNoInteractions(ragService);
		verifyNoInteractions(conversationService);
	}

	private boolean isValidUuid(String value) {
		try {
			UUID.fromString(value);
			return true;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}
}
