package com.ramanuj.docmind.dto;

import java.time.LocalDateTime;

import com.ramanuj.docmind.entity.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ChatMessage", description = "Individual message stored as part of a conversation.")
public record ChatMessageDto(@Schema(description = "Unique message identifier.", example = "101") Long id,
		@Schema(description = "Type of message indicating whether it was sent by the user or assistant.", example = "USER")
		MessageType messageType, @Schema(description = "Message content.", example = "Explain the key risks in the uploaded report.") String content, 
		@Schema(description = "Date and time when the message was created.", example = "2026-11-26T01:30:00") LocalDateTime createdAt) {
	
}
