
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

@ExtendWith(MockitoExtension.class)
class JpaChatMemoryTest {

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private ConversationRepository conversationRepository;

	@InjectMocks
	private JpaChatMemory jpaChatMemory;

	@Captor
	private ArgumentCaptor<ChatMessage> chatMessageCaptor;

	private Conversation conversation;

	private static final String CONVERSATION_ID = "conversation-123";

	@BeforeEach
	void setUp() {
		conversation = Conversation.builder().id(CONVERSATION_ID).title("Test conversation")
				.createdAt(LocalDateTime.now().minusDays(1)).updatedAt(LocalDateTime.now().minusHours(1)).build();
	}

	// ADD MESSAGE TESTS

	@Test
	void add_shouldReturnWhenMessagesAreNull() {

		jpaChatMemory.add(CONVERSATION_ID, (List<Message>) null);

		verifyNoInteractions(conversationRepository);
		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void add_shouldReturnWhenMessagesAreEmpty() {

		jpaChatMemory.add(CONVERSATION_ID, List.of());

		verifyNoInteractions(conversationRepository);
		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void add_shouldThrowExceptionWhenConversationDoesNotExist() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

		List<Message> messages = List.of(new UserMessage("Hello"));

		assertThrows(ResourceNotFoundException.class, () -> jpaChatMemory.add(CONVERSATION_ID, messages));

		verify(conversationRepository).findById(CONVERSATION_ID);
		verifyNoInteractions(chatMessageRepository);
	}

	@Test
	void add_shouldSaveUserMessage() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

		List<Message> messages = List.of(new UserMessage("Hello, how are you?"));

		LocalDateTime beforeExecution = LocalDateTime.now();

		jpaChatMemory.add(CONVERSATION_ID, messages);

		LocalDateTime afterExecution = LocalDateTime.now();

		verify(chatMessageRepository).save(chatMessageCaptor.capture());

		ChatMessage savedMessage = chatMessageCaptor.getValue();

		assertEquals(com.ramanuj.docmind.entity.MessageType.USER, savedMessage.getMessageType());
		assertEquals("Hello, how are you?", savedMessage.getContent());
		assertEquals(conversation, savedMessage.getConversation());

		assertNotNull(savedMessage.getCreatedAt());
		assertFalse(savedMessage.getCreatedAt().isBefore(beforeExecution));
		assertFalse(savedMessage.getCreatedAt().isAfter(afterExecution));

		assertNotNull(conversation.getUpdatedAt());
	}

	@Test
	void add_shouldSaveAssistantMessage() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

		List<Message> messages = List.of(new AssistantMessage("I am doing well."));

		jpaChatMemory.add(CONVERSATION_ID, messages);

		verify(chatMessageRepository).save(chatMessageCaptor.capture());

		ChatMessage savedMessage = chatMessageCaptor.getValue();

		assertEquals(com.ramanuj.docmind.entity.MessageType.ASSISTANT, savedMessage.getMessageType());
		assertEquals("I am doing well.", savedMessage.getContent());
		assertEquals(conversation, savedMessage.getConversation());
	}

	@Test
	void add_shouldSaveSystemMessage() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

		List<Message> messages = List.of(new SystemMessage("You are a helpful assistant."));

		jpaChatMemory.add(CONVERSATION_ID, messages);

		verify(chatMessageRepository).save(chatMessageCaptor.capture());

		ChatMessage savedMessage = chatMessageCaptor.getValue();

		assertEquals(com.ramanuj.docmind.entity.MessageType.SYSTEM, savedMessage.getMessageType());
		assertEquals("You are a helpful assistant.", savedMessage.getContent());
	}

	@Test
	void add_shouldSaveMultipleMessages() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

		List<Message> messages = List.of(new UserMessage("What is Spring Boot?"),
				new AssistantMessage("Spring Boot simplifies Spring application development."),
				new UserMessage("What about Spring AI?"));

		jpaChatMemory.add(CONVERSATION_ID, messages);

		verify(chatMessageRepository, times(3)).save(any(ChatMessage.class));

		verify(conversationRepository).findById(CONVERSATION_ID);

		assertNotNull(conversation.getUpdatedAt());
	}

	@Test
	void add_shouldUpdateConversationTimestamp() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

		LocalDateTime previousUpdatedAt = conversation.getUpdatedAt();

		jpaChatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Update conversation timestamp")));

		assertNotNull(conversation.getUpdatedAt());

		assertFalse(conversation.getUpdatedAt().isBefore(previousUpdatedAt));
	}

	@Test
	void add_shouldIgnoreUnsupportedMessageType() {

		when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

		Message unsupportedMessage = mock(Message.class);

		when(unsupportedMessage.getMessageType()).thenReturn(MessageType.TOOL);

		List<Message> messages = List.of(new UserMessage("Supported message"), unsupportedMessage);

		jpaChatMemory.add(CONVERSATION_ID, messages);

		verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));

		verify(chatMessageRepository, never())
				.save(org.mockito.ArgumentMatchers.argThat(savedMessage -> savedMessage.getContent() == null));

		assertNotNull(conversation.getUpdatedAt());
	}

	// GET MESSAGE TESTS

	@Test
	void get_shouldReturnEmptyListWhenNoMessagesExist() {

		when(chatMessageRepository.findLastNMessages(CONVERSATION_ID, 10)).thenReturn(List.of());

		List<Message> result = jpaChatMemory.get(CONVERSATION_ID);

		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(chatMessageRepository).findLastNMessages(CONVERSATION_ID, 10);
	}

	@Test
	void get_shouldRetrieveUserAssistantAndSystemMessages() {

		LocalDateTime now = LocalDateTime.now();

		List<ChatMessage> storedMessages = new ArrayList<>();

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.USER)
				.content("Hello").createdAt(now).build());

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.ASSISTANT)
				.content("Hello! How can I help?").createdAt(now.plusSeconds(1)).build());

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.SYSTEM)
				.content("You are a helpful assistant.").createdAt(now.plusSeconds(2)).build());

		when(chatMessageRepository.findLastNMessages(CONVERSATION_ID, 10)).thenReturn(storedMessages);

		List<Message> result = jpaChatMemory.get(CONVERSATION_ID);

		assertEquals(3, result.size());

		assertInstanceOf(UserMessage.class, result.get(0));
		assertEquals("Hello", result.get(0).getText());

		assertInstanceOf(AssistantMessage.class, result.get(1));
		assertEquals("Hello! How can I help?", result.get(1).getText());

		assertInstanceOf(SystemMessage.class, result.get(2));
		assertEquals("You are a helpful assistant.", result.get(2).getText());
	}

	@Test
	void get_shouldSortMessagesInAscendingCreatedAtOrder() {

		LocalDateTime now = LocalDateTime.now();

		List<ChatMessage> storedMessages = new ArrayList<>();

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.ASSISTANT)
				.content("Second message").createdAt(now.plusMinutes(2)).build());

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.USER)
				.content("First message").createdAt(now.plusMinutes(1)).build());

		when(chatMessageRepository.findLastNMessages(CONVERSATION_ID, 10)).thenReturn(storedMessages);

		List<Message> result = jpaChatMemory.get(CONVERSATION_ID);

		assertEquals(2, result.size());

		assertEquals("First message", result.get(0).getText());
		assertEquals("Second message", result.get(1).getText());
	}

	@Test
	void get_shouldPlaceNullTimestampsBeforeDatedMessages() {

		LocalDateTime now = LocalDateTime.now();

		List<ChatMessage> storedMessages = new ArrayList<>();

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.ASSISTANT)
				.content("Dated message").createdAt(now).build());

		storedMessages.add(ChatMessage.builder().messageType(com.ramanuj.docmind.entity.MessageType.USER)
				.content("Message without timestamp").createdAt(null).build());

		when(chatMessageRepository.findLastNMessages(CONVERSATION_ID, 10)).thenReturn(storedMessages);

		List<Message> result = jpaChatMemory.get(CONVERSATION_ID);

		assertEquals(2, result.size());

		assertEquals("Message without timestamp", result.get(0).getText());
		assertEquals("Dated message", result.get(1).getText());
	}

	@Test
	void get_shouldRequestOnlyLastTenMessages() {

		when(chatMessageRepository.findLastNMessages(CONVERSATION_ID, 10)).thenReturn(List.of());

		jpaChatMemory.get(CONVERSATION_ID);

		verify(chatMessageRepository).findLastNMessages(CONVERSATION_ID, 10);
	}

	// CLEAR MESSAGE TESTS

	@Test
	void clear_shouldDeleteMessagesForConversation() {

		jpaChatMemory.clear(CONVERSATION_ID);

		verify(chatMessageRepository).deleteByConversation_Id(CONVERSATION_ID);
	}
}
