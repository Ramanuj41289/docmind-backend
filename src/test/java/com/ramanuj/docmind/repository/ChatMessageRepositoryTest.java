package com.ramanuj.docmind.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;

@DataJpaTest
class ChatMessageRepositoryTest {

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private UserRepository userRepository;

	private Conversation conversation;
	private Conversation anotherConversation;

	private LocalDateTime baseTime;

	@BeforeEach
	void setUp() {
		User user = userRepository.save(User.builder().username("testuser").email("testuser@example.com")
				.password("password").role(Role.USER).build());

		baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);

		conversation = conversationRepository.save(Conversation.builder().id(UUID.randomUUID().toString()).user(user)
				.title("Test Conversation").createdAt(baseTime).updatedAt(baseTime).build());

		anotherConversation = conversationRepository.save(Conversation.builder().id(UUID.randomUUID().toString())
				.user(user).title("Another Conversation").createdAt(baseTime).updatedAt(baseTime).build());
	}

	@Test
	void findByConversationIdOrderByCreatedAtAscShouldReturnMessagesChronologically() {
		saveMessage("Third message", MessageType.USER, baseTime.plusMinutes(3));
		saveMessage("First message", MessageType.USER, baseTime.plusMinutes(1));
		saveMessage("Second message", MessageType.ASSISTANT, baseTime.plusMinutes(2));

		List<ChatMessage> result = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());

		assertThat(result).hasSize(3);
		assertThat(result.get(0).getContent()).isEqualTo("First message");
		assertThat(result.get(1).getContent()).isEqualTo("Second message");
		assertThat(result.get(2).getContent()).isEqualTo("Third message");
	}

	@Test
	void findByConversationIdShouldReturnEmptyListWhenNoMessagesExist() {
		List<ChatMessage> result = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());

		assertThat(result).isEmpty();
	}

	@Test
	void findByConversationIdShouldNotReturnMessagesFromAnotherConversation() {
		saveMessage("Conversation one message", MessageType.USER, baseTime.plusMinutes(1));

		saveMessage("Conversation two message", MessageType.ASSISTANT, baseTime.plusMinutes(2), anotherConversation);

		List<ChatMessage> result = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getContent()).isEqualTo("Conversation one message");
	}

	@Test
	void findLastNMessagesShouldReturnLatestMessagesInDescendingOrder() {
		saveMessage("First message", MessageType.USER, baseTime.plusMinutes(1));
		saveMessage("Second message", MessageType.ASSISTANT, baseTime.plusMinutes(2));
		saveMessage("Third message", MessageType.USER, baseTime.plusMinutes(3));
		saveMessage("Fourth message", MessageType.ASSISTANT, baseTime.plusMinutes(4));

		List<ChatMessage> result = chatMessageRepository.findLastNMessages(conversation.getId(), 2);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getContent()).isEqualTo("Fourth message");
		assertThat(result.get(1).getContent()).isEqualTo("Third message");
	}

	@Test
	void findLastNMessagesShouldReturnAllMessagesWhenLimitExceedsAvailableMessages() {
		saveMessage("First message", MessageType.USER, baseTime.plusMinutes(1));
		saveMessage("Second message", MessageType.ASSISTANT, baseTime.plusMinutes(2));

		List<ChatMessage> result = chatMessageRepository.findLastNMessages(conversation.getId(), 10);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getContent()).isEqualTo("Second message");
		assertThat(result.get(1).getContent()).isEqualTo("First message");
	}

	@Test
	void findLastNMessagesShouldReturnEmptyListWhenNoMessagesExist() {
		List<ChatMessage> result = chatMessageRepository.findLastNMessages(conversation.getId(), 5);

		assertThat(result).isEmpty();
	}

	@Test
	void findLastNMessagesShouldUseIdAsTieBreakerForSameTimestamp() {
		saveMessage("First inserted", MessageType.USER, baseTime);
		saveMessage("Second inserted", MessageType.ASSISTANT, baseTime);

		List<ChatMessage> result = chatMessageRepository.findLastNMessages(conversation.getId(), 2);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getContent()).isEqualTo("Second inserted");
		assertThat(result.get(1).getContent()).isEqualTo("First inserted");
	}

	@Test
	void deleteByConversationIdShouldDeleteOnlyMatchingConversationMessages() {
		saveMessage("First conversation message", MessageType.USER, baseTime.plusMinutes(1));

		saveMessage("Second conversation message", MessageType.ASSISTANT, baseTime.plusMinutes(2), anotherConversation);

		chatMessageRepository.deleteByConversation_Id(conversation.getId());

		List<ChatMessage> deletedConversationMessages = chatMessageRepository
				.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());

		List<ChatMessage> remainingMessages = chatMessageRepository
				.findByConversation_IdOrderByCreatedAtAsc(anotherConversation.getId());

		assertThat(deletedConversationMessages).isEmpty();
		assertThat(remainingMessages).hasSize(1);
		assertThat(remainingMessages.get(0).getContent()).isEqualTo("Second conversation message");
	}

	private void saveMessage(String content, MessageType messageType, LocalDateTime createdAt) {

		saveMessage(content, messageType, createdAt, conversation);
	}

	private void saveMessage(String content, MessageType messageType, LocalDateTime createdAt,
			Conversation targetConversation) {

		ChatMessage message = ChatMessage.builder().conversation(targetConversation).messageType(messageType)
				.content(content).createdAt(createdAt).build();

		chatMessageRepository.save(message);
	}
}