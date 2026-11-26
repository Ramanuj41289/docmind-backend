
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;

@DataJpaTest
@Import(JpaChatMemory.class)
class JpaChatMemoryIntegrationTest {

	@Autowired
	private JpaChatMemory chatMemory;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	private Conversation conversation;

	@BeforeEach
	void setUp() {

		User user = User.builder().username("chatmemoryuser").email("chatmemory@example.com")
				.password("encoded-password").role(Role.USER).build();

		entityManager.persistAndFlush(user);

		conversation = Conversation.builder().id("integration-conversation-1").user(user)
				.title("Integration Test Conversation").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.build();

		entityManager.persistAndFlush(conversation);
	}

	@Test
	void addShouldPersistUserAssistantAndSystemMessages() {

		List<Message> messages = List.of(new UserMessage("Hello"), new AssistantMessage("Hello! How can I help?"),
				new SystemMessage("You are a helpful assistant"));

		chatMemory.add(conversation.getId(), messages);

		entityManager.flush();
		entityManager.clear();

		List<ChatMessage> storedMessages = chatMessageRepository
				.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());

		assertThat(storedMessages).hasSize(3);

		assertThat(storedMessages.get(0).getMessageType()).isEqualTo(MessageType.USER);
		assertThat(storedMessages.get(0).getContent()).isEqualTo("Hello");

		assertThat(storedMessages.get(1).getMessageType()).isEqualTo(MessageType.ASSISTANT);
		assertThat(storedMessages.get(1).getContent()).isEqualTo("Hello! How can I help?");

		assertThat(storedMessages.get(2).getMessageType()).isEqualTo(MessageType.SYSTEM);
		assertThat(storedMessages.get(2).getContent()).isEqualTo("You are a helpful assistant");
	}

	@Test
	void getShouldReturnMessagesInChronologicalOrder() throws InterruptedException {

		chatMemory.add(conversation.getId(), List.of(new UserMessage("First message")));

		Thread.sleep(5);

		chatMemory.add(conversation.getId(), List.of(new AssistantMessage("Second message")));

		Thread.sleep(5);

		chatMemory.add(conversation.getId(), List.of(new UserMessage("Third message")));

		List<Message> messages = chatMemory.get(conversation.getId());

		assertThat(messages).hasSize(3);
		assertThat(messages.get(0).getText()).isEqualTo("First message");
		assertThat(messages.get(1).getText()).isEqualTo("Second message");
		assertThat(messages.get(2).getText()).isEqualTo("Third message");
	}

	@Test
	void getShouldReturnOnlyLatestTenMessages() throws InterruptedException {

		for (int i = 1; i <= 12; i++) {

			chatMemory.add(conversation.getId(), List.of(new UserMessage("Message " + i)));

			Thread.sleep(5);
		}

		List<Message> messages = chatMemory.get(conversation.getId());

		assertThat(messages).hasSize(10);
		assertThat(messages.get(0).getText()).isEqualTo("Message 3");
		assertThat(messages.get(9).getText()).isEqualTo("Message 12");
	}

	@Test
	void getShouldReturnEmptyListWhenNoMessagesExist() {

		List<Message> messages = chatMemory.get(conversation.getId());

		assertThat(messages).isEmpty();
	}

	@Test
	void addShouldIgnoreNullOrEmptyMessageLists() {

		chatMemory.add(conversation.getId(), (List<Message>) null);
		chatMemory.add(conversation.getId(), List.of());

		assertThat(chatMessageRepository.count()).isZero();
	}

	@Test
	void addShouldThrowExceptionWhenConversationDoesNotExist() {

		assertThatThrownBy(() -> chatMemory.add("missing-conversation", List.of(new UserMessage("Hello"))))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Conversation not found");
	}

	@Test
	void clearShouldDeleteAllMessagesForConversation() {

		chatMemory.add(conversation.getId(), List.of(new UserMessage("Question"), new AssistantMessage("Answer")));

		assertThat(chatMessageRepository.count()).isEqualTo(2);

		chatMemory.clear(conversation.getId());

		assertThat(chatMessageRepository.count()).isZero();
	}
}
