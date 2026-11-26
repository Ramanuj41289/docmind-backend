
package com.ramanuj.docmind.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaChatMemory  {

	private final ChatMessageRepository chatMessageRepository;
	private final ConversationRepository conversationRepository;

	@Transactional
	public void add(String conversationId, List<Message> messages) {

		if (messages == null || messages.isEmpty()) {
			return;
		}

		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

		for (Message message : messages) {

			MessageType type = switch (message.getMessageType()) {
			case USER -> MessageType.USER;
			case ASSISTANT -> MessageType.ASSISTANT;
			case SYSTEM -> MessageType.SYSTEM;
			default -> null;
			};

			if (type == null) {
				continue;
			}

			chatMessageRepository.save(ChatMessage.builder().conversation(conversation).messageType(type)
					.content(message.getText()).createdAt(LocalDateTime.now()).build());
		}

		conversation.setUpdatedAt(LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public List<Message> get(String conversationId) {
		List<ChatMessage> stored = chatMessageRepository.findLastNMessages(conversationId, 10);
		if (stored.isEmpty()) {
			return Collections.emptyList();
		}
		stored.sort(Comparator.comparing((@NonNull ChatMessage message) -> message.getCreatedAt(), Comparator.nullsFirst(Comparator.naturalOrder())));
		List<Message> messages = new ArrayList<>(stored.size());
		for (ChatMessage item : stored) {
			switch (item.getMessageType()) {
				case USER -> messages.add(new UserMessage(item.getContent()));
				case ASSISTANT -> messages.add(new AssistantMessage(item.getContent()));
				case SYSTEM -> messages.add(new SystemMessage(item.getContent()));
			}
		}
		return messages;
	}

	@Transactional
	public void clear(String conversationId) {
		chatMessageRepository.deleteByConversation_Id(conversationId);
	}
}
