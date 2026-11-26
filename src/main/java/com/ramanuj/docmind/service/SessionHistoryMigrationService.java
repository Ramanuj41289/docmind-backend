
package com.ramanuj.docmind.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.EventType;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionAttributes;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.stereotype.Service;

import com.ramanuj.docmind.entity.ChatMessage;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.MessageType;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.ChatMessageRepository;
import com.ramanuj.docmind.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionHistoryMigrationService {
	private static final String LEGACY_IMPORT_EVENT = "DOCMIND.LEGACY_IMPORTED";
	private static final String LEGACY_EVENT_ID_PREFIX = "docmind-legacy-message-";
	private static final String MIGRATION_MARKER_ID_PREFIX = "docmind-legacy-import-marker-";
	private final SessionService sessionService;
	private final ChatMessageRepository chatMessageRepository;
	private final ConversationRepository conversationRepository;

	/**
	 * Creates the Spring AI session if required, verifies ownership, and imports
	 * the existing JPA conversation history once.
	 */

	public void ensureSessionReady(String conversationId, User user) {
		validateUser(user);
		Conversation conversation = conversationRepository.findByIdAndUser(conversationId, user).orElseThrow(() -> 
			new ResourceNotFoundException("Conversation not found"));
		Session session = sessionService.findOrCreate(CreateSessionRequest.builder().id(conversationId).metadata(SessionAttributes.PRINCIPAL_NAME, 
				user.getUsername()).build());
		validateSessionOwnership(session, user);
		if (hasLegacyImportMarker(conversationId)) {
			return;
		}
		importLegacyMessages(conversation);
		appendMigrationMarker(conversationId);
		log.info("Imported legacy conversation history into Session Memory: conversationId={}", conversationId);
	}

	private void importLegacyMessages(Conversation conversation) {
		List<ChatMessage> messages = chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());
		for (ChatMessage storedMessage : messages) {
			Message message = convertMessage(storedMessage);

			// The legacy chat-memory implementation could store system messages. These are not imported as conversation turns.
			if (message == null) {
				continue;
			}
			Instant timestamp = toInstant(storedMessage.getCreatedAt());
			SessionEvent event = SessionEvent.builder().id(LEGACY_EVENT_ID_PREFIX + storedMessage.getId()).sessionId(conversation.getId()).timestamp(timestamp)
					.message(message).metadata("source", "legacy-chat-messages").metadata("legacyMessageId", storedMessage.getId()).build();

			// Session repositories treat an existing event ID as an idempotent retry, avoiding duplicate imports.
			sessionService.appendEvent(event);
		}
	}

	private Message convertMessage(ChatMessage storedMessage) {
		MessageType type = storedMessage.getMessageType();
		if (type == null) {
			return null;
		}
		return switch (type) {
			case USER -> new UserMessage(storedMessage.getContent());
			case ASSISTANT -> new AssistantMessage(storedMessage.getContent());
			case SYSTEM -> null;
		};
	}

	private Instant toInstant(LocalDateTime createdAt) {
		if (createdAt == null) {
			return Instant.now();
		}
		return createdAt.atZone(ZoneId.systemDefault()).toInstant();
	}

	private boolean hasLegacyImportMarker(String conversationId) {
		return sessionService.getEvents(conversationId).stream().anyMatch(event -> LEGACY_IMPORT_EVENT.equals(event.getType().name()));
	}

	private void appendMigrationMarker(String conversationId) {
		SessionEvent marker = SessionEvent.builder().id(MIGRATION_MARKER_ID_PREFIX + conversationId).sessionId(conversationId)
				.type(EventType.of(LEGACY_IMPORT_EVENT)).payload("source", "chat_messages").payload("migrationVersion", 1).build();
		sessionService.appendEvent(marker);
	}

	private void validateSessionOwnership(Session session, User user) {
		String owner = SessionAttributes.principalName(session);
		if (owner == null || !owner.equals(user.getUsername())) {
			throw new ResourceNotFoundException("Conversation not found");
		}
	}

	private void validateUser(User user) {
		if (user == null || user.getId() == null || user.getUsername() == null || user.getUsername().isBlank()) {
			throw new IllegalArgumentException("Authenticated user is required");
		}
	}
}
