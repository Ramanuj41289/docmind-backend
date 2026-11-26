
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.session.SessionService;

import com.ramanuj.docmind.dto.ChatMessageDto;
import com.ramanuj.docmind.dto.ConversationDto;
import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private SessionService sessionService;

	@InjectMocks
	private ConversationService conversationService;

	private User user;
	private User anotherUser;
	private Conversation conversation;

	private final LocalDateTime createdAt = LocalDateTime.of(2026, 11, 20, 10, 0);

	private final LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 20, 11, 0);

	@BeforeEach
	void setUp() {

		user = User.builder().id(1L).username("ramanuj").email("ramanuj@example.com").password("encoded-password")
				.build();

		anotherUser = User.builder().id(2L).username("anotheruser").email("another@example.com")
				.password("encoded-password").build();

		conversation = Conversation.builder().id("conversation-123").user(user).title("Java Discussion")
				.createdAt(createdAt).updatedAt(updatedAt).build();
	}

	@Test
	void getUserConversationsShouldReturnUserConversations() {

		when(conversationRepository.findByUserOrderByUpdatedAtDesc(user)).thenReturn(List.of(conversation));

		List<ConversationDto> result = conversationService.getUserConversations(user);

		assertEquals(1, result.size());
		assertEquals("conversation-123", result.get(0).id());
		assertEquals("Java Discussion", result.get(0).title());
		assertEquals(createdAt, result.get(0).createdAt());
		assertEquals(updatedAt, result.get(0).updatedAt());

		verify(conversationRepository).findByUserOrderByUpdatedAtDesc(user);
	}

	@Test
	void getUserConversationsShouldReturnEmptyList() {

		when(conversationRepository.findByUserOrderByUpdatedAtDesc(user)).thenReturn(List.of());

		List<ConversationDto> result = conversationService.getUserConversations(user);

		assertTrue(result.isEmpty());
	}

	@Test
	void getUserConversationsShouldRejectNullUser() {

		assertThrows(IllegalArgumentException.class, () -> conversationService.getUserConversations(null));

		verifyNoInteractions(conversationRepository);
	}

	@Test
	void getUserConversationsShouldRejectUserWithoutId() {

		User invalidUser = User.builder().username("invalid").email("invalid@example.com").build();

		assertThrows(IllegalArgumentException.class, () -> conversationService.getUserConversations(invalidUser));

		verifyNoInteractions(conversationRepository);
	}

	@Test
	void getConversationMessagesShouldReturnMessages() {

		ChatMessage userMessage = ChatMessage.builder().id(1L).conversation(conversation).messageType(MessageType.USER)
				.content("Explain Spring Boot").createdAt(createdAt).build();

		ChatMessage assistantMessage = ChatMessage.builder().id(2L).conversation(conversation)
				.messageType(MessageType.ASSISTANT).content("Spring Boot simplifies application development.")
				.createdAt(updatedAt).build();

		when(conversationRepository.findByIdAndUser("conversation-123", user)).thenReturn(Optional.of(conversation));

		when(chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc("conversation-123"))
				.thenReturn(List.of(userMessage, assistantMessage));

		List<ChatMessageDto> result = conversationService.getConversationMessages("conversation-123", user);

		assertEquals(2, result.size());

		assertEquals(MessageType.USER, result.get(0).messageType());
		assertEquals("Explain Spring Boot", result.get(0).content());

		assertEquals(MessageType.ASSISTANT, result.get(1).messageType());
		assertEquals("Spring Boot simplifies application development.", result.get(1).content());

		verify(chatMessageRepository).findByConversation_IdOrderByCreatedAtAsc("conversation-123");
	}

	@Test
	void getConversationMessagesShouldReturnEmptyListWhenNoMessagesExist() {

		when(conversationRepository.findByIdAndUser("conversation-123", user)).thenReturn(Optional.of(conversation));

		when(chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc("conversation-123")).thenReturn(List.of());

		List<ChatMessageDto> result = conversationService.getConversationMessages("conversation-123", user);

		assertTrue(result.isEmpty());
	}

	@Test
	void getConversationMessagesShouldRejectAnotherUsersConversation() {

		when(conversationRepository.findByIdAndUser("conversation-123", anotherUser)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> conversationService.getConversationMessages("conversation-123", anotherUser));

		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void getConversationMessagesShouldRejectNonexistentConversation() {

		when(conversationRepository.findByIdAndUser("unknown-id", user)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> conversationService.getConversationMessages("unknown-id", user));

		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void getConversationMessagesShouldRejectNullUser() {

		assertThrows(IllegalArgumentException.class,
				() -> conversationService.getConversationMessages("conversation-123", null));

		verifyNoInteractions(conversationRepository);
		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void deleteConversationShouldDeleteOwnedConversation() {

		when(conversationRepository.findByIdAndUser("conversation-123", user)).thenReturn(Optional.of(conversation));

		conversationService.deleteConversation("conversation-123", user);

		verify(conversationRepository).delete(conversation);
	}

	@Test
	void deleteConversationShouldRejectAnotherUsersConversation() {

		when(conversationRepository.findByIdAndUser("conversation-123", anotherUser)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> conversationService.deleteConversation("conversation-123", anotherUser));

		verify(conversationRepository, never()).delete(any(Conversation.class));
	}

	@Test
	void deleteConversationShouldRejectNonexistentConversation() {

		when(conversationRepository.findByIdAndUser("unknown-id", user)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> conversationService.deleteConversation("unknown-id", user));

		verify(conversationRepository, never()).delete(any(Conversation.class));
	}

	@Test
	void deleteConversationShouldRejectNullUser() {

		assertThrows(IllegalArgumentException.class,
				() -> conversationService.deleteConversation("conversation-123", null));

		verifyNoInteractions(conversationRepository);
	}
}
