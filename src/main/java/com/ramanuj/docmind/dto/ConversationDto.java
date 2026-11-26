
package com.ramanuj.docmind.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Conversation", description = "Conversation metadata used to identify and display a user's chat history.")
public record ConversationDto(
		@Schema(description = "Unique conversation identifier.", example = "conv-7f42d1") String id,
		@Schema(description = "Conversation title.", example = "Annual Report Analysis") String title,
		@Schema(description = "Date and time when the conversation was created.", example = "2026-11-25T14:30:00") LocalDateTime createdAt,
		@Schema(description = "Date and time when the conversation was last updated.", example = "2026-11-26T01:30:00") LocalDateTime updatedAt) {
}
