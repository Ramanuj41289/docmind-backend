
package com.ramanuj.docmind.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramanuj.docmind.dto.ChatMessageDto;
import com.ramanuj.docmind.dto.ChatRequestDto;
import com.ramanuj.docmind.dto.ChatResponseDto;
import com.ramanuj.docmind.dto.ConversationDto;
import com.ramanuj.docmind.dto.SearchRequestDto;
import com.ramanuj.docmind.dto.SearchResultDto;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.GlobalExceptionHandler;
import com.ramanuj.docmind.service.ConversationService;
import com.ramanuj.docmind.service.RagService;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

	@Mock
	private RagService ragService;

	@Mock
	private ConversationService conversationService;

	@InjectMocks
	private ChatController chatController;

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	private User user;

	private Authentication authentication;

	@BeforeEach
	void setUp() {

		objectMapper = new ObjectMapper();

		mockMvc = MockMvcBuilders.standaloneSetup(chatController).setControllerAdvice(new GlobalExceptionHandler())
				.build();

		user = User.builder().id(1L).username("testuser").email("testuser@example.com").password("password")
				.role(Role.USER).build();

		authentication = new UsernamePasswordAuthenticationToken(user, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void getConversations_shouldReturnUserConversations() throws Exception {

		LocalDateTime now = LocalDateTime.now();

		ConversationDto firstConversation = new ConversationDto("conversation-1", "Spring Boot discussion",
				now.minusDays(1), now);

		ConversationDto secondConversation = new ConversationDto("conversation-2", "Java discussion", now.minusHours(2),
				now);

		when(conversationService.getUserConversations(user)).thenReturn(List.of(firstConversation, secondConversation));

		mockMvc.perform(get("/api/v1/chat/conversations").principal(authentication)).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Conversations retrieved successfully"))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].id").value("conversation-1"))
				.andExpect(jsonPath("$.data[0].title").value("Spring Boot discussion"))
				.andExpect(jsonPath("$.data[1].id").value("conversation-2"))
				.andExpect(jsonPath("$.data[1].title").value("Java discussion"))
				.andExpect(jsonPath("$.timestamp").exists());

		verify(conversationService).getUserConversations(user);
	}

	@Test
	void getConversations_shouldReturnEmptyListWhenNoConversationsExist() throws Exception {

		when(conversationService.getUserConversations(user)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/chat/conversations").principal(authentication)).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.length()").value(0));

		verify(conversationService).getUserConversations(user);
	}

	@Test
	void getConversationMessages_shouldReturnMessages() throws Exception {

		LocalDateTime now = LocalDateTime.now();

		List<ChatMessageDto> messages = List.of(
				new ChatMessageDto(1L, MessageType.USER, "What is Spring Boot?", now.minusMinutes(2)),
				new ChatMessageDto(2L, MessageType.ASSISTANT, "Spring Boot simplifies Spring application development.",
						now.minusMinutes(1)));

		when(conversationService.getConversationMessages("conversation-1", user)).thenReturn(messages);

		mockMvc.perform(
				get("/api/v1/chat/conversations/{conversationId}/messages", "conversation-1").principal(authentication))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Conversation messages retrieved successfully"))
				.andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].messageType").value("USER"))
				.andExpect(jsonPath("$.data[0].content").value("What is Spring Boot?"))
				.andExpect(jsonPath("$.data[1].messageType").value("ASSISTANT")).andExpect(
						jsonPath("$.data[1].content").value("Spring Boot simplifies Spring application development."));

		verify(conversationService).getConversationMessages("conversation-1", user);
	}

	@Test
	void deleteConversation_shouldReturnSuccessResponse() throws Exception {

		mockMvc.perform(
				delete("/api/v1/chat/conversations/{conversationId}", "conversation-1").principal(authentication))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Conversation deleted successfully"))
				.andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.timestamp").exists());

		verify(conversationService).deleteConversation("conversation-1", user);
	}

	@Test
	void askQuestion_shouldReturnChatResponse() throws Exception {

		ChatRequestDto request = ChatRequestDto.builder().question("What is Spring Boot?").topK(5).build();

		ChatResponseDto response = ChatResponseDto.builder()
				.answer("Spring Boot simplifies Spring application development.").conversationId("conversation-1")
				.citations(List.of()).responseTimeMs(150L).build();

		when(ragService.askQuestion(any(ChatRequestDto.class), eq(user))).thenReturn(response);

		mockMvc.perform(post("/api/v1/chat/query").principal(authentication).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.answer").value("Spring Boot simplifies Spring application development."))
				.andExpect(jsonPath("$.data.conversationId").value("conversation-1"))
				.andExpect(jsonPath("$.data.responseTimeMs").value(150)).andExpect(jsonPath("$.timestamp").exists());

		verify(ragService).askQuestion(any(ChatRequestDto.class), eq(user));
	}

	@Test
	void searchSimilar_shouldReturnMatchingChunks() throws Exception {

		SearchRequestDto request = SearchRequestDto.builder().query("Spring Boot").topK(5).build();

		SearchResultDto response = SearchResultDto.builder().query("Spring Boot").totalMatches(0).matches(List.of())
				.build();

		when(ragService.searchSimilarChunks(any(SearchRequestDto.class), eq(user))).thenReturn(response);

		mockMvc.perform(post("/api/v1/chat/search/similarity").principal(authentication)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.query").value("Spring Boot"))
				.andExpect(jsonPath("$.data.totalMatches").value(0))
				.andExpect(jsonPath("$.data.matches.length()").value(0));

		verify(ragService).searchSimilarChunks(any(SearchRequestDto.class), eq(user));
	}

	@Test
	void getConversations_shouldReturnForbiddenWhenAuthenticationIsMissing() throws Exception {

		mockMvc.perform(get("/api/v1/chat/conversations")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));

		verifyNoInteractions(conversationService);
	}

	@Test
	void askQuestion_shouldReturnBadRequestWhenQuestionIsBlank() throws Exception {

		String invalidRequest = """
				{
				    "question": "   "
				}
				""";

		mockMvc.perform(post("/api/v1/chat/query").principal(authentication).contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequest)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data.question").value("Question cannot be empty"));

		verifyNoInteractions(ragService);
	}
}
