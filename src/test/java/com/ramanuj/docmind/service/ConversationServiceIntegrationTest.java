
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.ramanuj.docmind.dto.ChatMessageDto;
import com.ramanuj.docmind.dto.ConversationDto;
import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

@DataJpaTest
@Import(ConversationService.class)
class ConversationServiceIntegrationTest {

	@Autowired
	private ConversationService conversationService;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	private User user;
	private User anotherUser;

	@BeforeEach
	void setUp() {

		user = User.builder().username("conversationuser").email("conversationuser@example.com")
				.password("encoded-password").role(Role.USER).build();

		anotherUser = User.builder().username("anotherconversationuser").email("anotherconversationuser@example.com")
				.password("encoded-password").role(Role.USER).build();

		entityManager.persistAndFlush(user);
		entityManager.persistAndFlush(anotherUser);
	}

	@Test
	void getUserConversationsShouldReturnOnlyCurrentUsersConversations() {

		Conversation first = createConversation("conversation-1", "First conversation", user,
				LocalDateTime.now().minusMinutes(10));

		Conversation second = createConversation("conversation-2", "Second conversation", user, LocalDateTime.now());

		createConversation("conversation-3", "Another user's conversation", anotherUser, LocalDateTime.now());

		List<ConversationDto> result = conversationService.getUserConversations(user);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).id()).isEqualTo(second.getId());
		assertThat(result.get(1).id()).isEqualTo(first.getId());

		assertThat(result.get(0).title()).isEqualTo("Second conversation");
		assertThat(result.get(1).title()).isEqualTo("First conversation");
	}

	@Test
	void getUserConversationsShouldReturnEmptyListWhenNoConversationsExist() {

		List<ConversationDto> result = conversationService.getUserConversations(user);

		assertThat(result).isEmpty();
	}

	@Test
	void getConversationMessagesShouldReturnMessagesInChronologicalOrder() {

		Conversation conversation = createConversation("message-conversation", "Message test", user,
				LocalDateTime.now());

		LocalDateTime now = LocalDateTime.now();

		createMessage(conversation, MessageType.ASSISTANT, "Second message", now.plusMinutes(2));

		createMessage(conversation, MessageType.USER, "First message", now);

		createMessage(conversation, MessageType.USER, "Third message", now.plusMinutes(4));

		entityManager.flush();
		entityManager.clear();

		List<ChatMessageDto> result = conversationService.getConversationMessages(conversation.getId(), user);

		assertThat(result).hasSize(3);

		assertThat(result.get(0).content()).isEqualTo("First message");
		assertThat(result.get(0).messageType()).isEqualTo(MessageType.USER);

		assertThat(result.get(1).content()).isEqualTo("Second message");
		assertThat(result.get(1).messageType()).isEqualTo(MessageType.ASSISTANT);

		assertThat(result.get(2).content()).isEqualTo("Third message");
		assertThat(result.get(2).messageType()).isEqualTo(MessageType.USER);
	}

	@Test
	void getConversationMessagesShouldReturnEmptyListWhenNoMessagesExist() {

		Conversation conversation = createConversation("empty-conversation", "Empty conversation", user,
				LocalDateTime.now());

		List<ChatMessageDto> result = conversationService.getConversationMessages(conversation.getId(), user);

		assertThat(result).isEmpty();
	}

	@Test
	void getConversationMessagesShouldRejectAnotherUsersConversation() {

		Conversation conversation = createConversation("private-conversation", "Private conversation", anotherUser,
				LocalDateTime.now());

		assertThatThrownBy(() -> conversationService.getConversationMessages(conversation.getId(), user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Conversation not found");
	}

	@Test
	void getConversationMessagesShouldRejectUnknownConversation() {

		assertThatThrownBy(() -> conversationService.getConversationMessages("missing-conversation", user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Conversation not found");
	}

	@Test
	void deleteConversationShouldDeleteConversationAndItsMessages() {

		Conversation conversation = createConversation("delete-conversation", "Conversation to delete", user,
				LocalDateTime.now());

		createMessage(conversation, MessageType.USER, "Question", LocalDateTime.now());

		createMessage(conversation, MessageType.ASSISTANT, "Answer", LocalDateTime.now().plusMinutes(1));

		entityManager.flush();

		assertThat(conversationRepository.existsById(conversation.getId())).isTrue();
		assertThat(chatMessageRepository.count()).isEqualTo(2);

		String conversationId = conversation.getId();

		// Detach existing entities so the service reloads the persisted
		// conversation and its messages before performing the cascade delete.
		entityManager.clear();

		conversationService.deleteConversation(conversationId, user);

		entityManager.flush();
		entityManager.clear();

		assertThat(conversationRepository.existsById(conversationId)).isFalse();
		assertThat(chatMessageRepository.count()).isZero();
	}

	@Test
	void deleteConversationShouldRejectAnotherUsersConversation() {

		Conversation conversation = createConversation("protected-conversation", "Protected conversation", anotherUser,
				LocalDateTime.now());

		assertThatThrownBy(() -> conversationService.deleteConversation(conversation.getId(), user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Conversation not found");

		assertThat(conversationRepository.existsById(conversation.getId())).isTrue();
	}

	@Test
	void serviceMethodsShouldRejectNullUser() {

		assertThatThrownBy(() -> conversationService.getUserConversations(null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");

		assertThatThrownBy(() -> conversationService.getConversationMessages("conversation-1", null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");

		assertThatThrownBy(() -> conversationService.deleteConversation("conversation-1", null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");
	}

	@Test
	void serviceMethodsShouldRejectUserWithoutDatabaseId() {

		User unsavedUser = User.builder().username("unsaveduser").email("unsaveduser@example.com")
				.password("encoded-password").role(Role.USER).build();

		assertThatThrownBy(() -> conversationService.getUserConversations(unsavedUser))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");

		assertThatThrownBy(() -> conversationService.getConversationMessages("conversation-1", unsavedUser))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");

		assertThatThrownBy(() -> conversationService.deleteConversation("conversation-1", unsavedUser))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");
	}

	private Conversation createConversation(String id, String title, User owner, LocalDateTime updatedAt) {

		Conversation conversation = Conversation.builder().id(id).title(title).user(owner).createdAt(updatedAt)
				.updatedAt(updatedAt).build();

		return entityManager.persistAndFlush(conversation);
	}

	private void createMessage(Conversation conversation, MessageType messageType, String content,
			LocalDateTime createdAt) {

		ChatMessage message = ChatMessage.builder().conversation(conversation).messageType(messageType).content(content)
				.createdAt(createdAt).build();

		entityManager.persist(message);
	}
}
