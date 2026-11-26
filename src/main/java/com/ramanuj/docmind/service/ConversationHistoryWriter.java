
package com.ramanuj.docmind.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationHistoryWriter {

	private final ChatMessageRepository chatMessageRepository;
	private final ConversationRepository conversationRepository;

	@Transactional
	public void saveUserMessage(String conversationId, String content) {
		saveMessage(conversationId, MessageType.USER, content);
	}

	@Transactional
	public void saveAssistantMessage(String conversationId, String content) {
		saveMessage(conversationId, MessageType.ASSISTANT, content);
	}

	private void saveMessage(String conversationId, MessageType messageType, String content) {

		if (conversationId == null || conversationId.isBlank()) {
			throw new IllegalArgumentException("Conversation ID is required");
		}

		if (content == null || content.isBlank()) {
			return;
		}

		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new IllegalStateException("Conversation not found: " + conversationId));

		LocalDateTime now = LocalDateTime.now();

		ChatMessage message = ChatMessage.builder().conversation(conversation).messageType(messageType).content(content)
				.createdAt(now).build();

		chatMessageRepository.save(message);

		// The conversation is managed by the current JPA transaction. Hibernate automatically persists this change.
		conversation.setUpdatedAt(now);
	}
}
