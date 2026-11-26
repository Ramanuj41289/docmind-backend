
package com.ramanuj.docmind.service;

import java.util.List;

import org.springframework.ai.session.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ramanuj.docmind.dto.ChatMessageDto;
import com.ramanuj.docmind.dto.ConversationDto;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConversationService {
	private final ConversationRepository conversationRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final SessionService sessionService;

	public List<ConversationDto> getUserConversations(User user) {
		validateUser(user);
		return conversationRepository.findByUserOrderByUpdatedAtDesc(user).stream().map(this::toConversationDto)
				.toList();
	}

	public List<ChatMessageDto> getConversationMessages(String conversationId, User user) {
		validateUser(user);
		validateConversationId(conversationId);
		Conversation conversation = findOwnedConversation(conversationId, user);
		return chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId()).stream()
				.map(message -> new ChatMessageDto(message.getId(), message.getMessageType(), message.getContent(),
						message.getCreatedAt()))
				.toList();
	}

	@Transactional
	public void deleteConversation(String conversationId, User user) {
		validateUser(user);
		validateConversationId(conversationId);
		Conversation conversation = findOwnedConversation(conversationId, user);

		// Delete the JPA conversation and its messages first. CascadeType.ALL and
		// orphanRemoval handle message deletion.
		conversationRepository.delete(conversation);

		// Delete Session Memory only after the JPA transaction commits.
		registerSessionCleanup(conversationId);
	}

	private void registerSessionCleanup(String conversationId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

				@Override
				public void afterCommit() {
					cleanupSession(conversationId);
				}
			});
		} else {
			cleanupSession(conversationId);
		}
	}

	private void cleanupSession(String conversationId) {
		try {
			var session = sessionService.findById(conversationId);
			if (session != null) {
				sessionService.delete(conversationId);
			}
		} catch (Exception ex) {
			log.error("Failed to delete Session Memory after conversation deletion: conversationId={}",
					conversationId, ex);
		}
	}

	private Conversation findOwnedConversation(String conversationId, User user) {
		return conversationRepository.findByIdAndUser(conversationId, user)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
	}

	private ConversationDto toConversationDto(Conversation conversation) {
		return new ConversationDto(conversation.getId(), conversation.getTitle(), conversation.getCreatedAt(),
				conversation.getUpdatedAt());
	}

	private void validateConversationId(String conversationId) {
		if (conversationId == null || conversationId.isBlank()) {
			throw new IllegalArgumentException("Conversation ID is required");
		}
	}

	private void validateUser(User user) {
		if (user == null || user.getId() == null) {
			throw new IllegalArgumentException("Authenticated user is required");
		}
	}
}
