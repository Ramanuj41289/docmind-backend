package com.ramanuj.docmind.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramanuj.docmind.dto.ApiErrorResponse;
import com.ramanuj.docmind.dto.ChatMessageDto;
import com.ramanuj.docmind.dto.ChatRequestDto;
import com.ramanuj.docmind.dto.ChatResponseDto;
import com.ramanuj.docmind.dto.ConversationDto;
import com.ramanuj.docmind.dto.SearchRequestDto;
import com.ramanuj.docmind.dto.SearchResultDto;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.service.ConversationService;
import com.ramanuj.docmind.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

	private final RagService ragService;
	private final ConversationService conversationService;

	private User requireUser(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof User user) {
			return user;
		}

		throw new AccessDeniedException("Authenticated user is required");
	}

	// Added structured logging for key operations
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChatController.class);

	@Operation(summary = "Ask a question", description = "Submits a question to the document assistant and returns an answer with source citations.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Question answered successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Referenced document or conversation not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@PostMapping("/query")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<ChatResponseDto>> askQuestion(
			@Valid @RequestBody ChatRequestDto request, Authentication authentication) {
		logger.info("Received askQuestion request for userId={}, conversationId={}", requireUser(authentication).getId(),
				request.getConversationId());

		ChatResponseDto result = ragService.askQuestion(request, requireUser(authentication));

		logger.info("askQuestion completed for userId={}, conversationId={}", requireUser(authentication).getId(),
				request.getConversationId());
		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<ChatResponseDto>builder().success(true)
				.data(result).timestamp(LocalDateTime.now()).build());
	}

	@Operation(summary = "Stream an answer", description = "Streams the document assistant's answer as server-sent events. The conversation ID is returned in the X-Conversation-Id response header.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Answer stream started successfully", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string"))),
			@ApiResponse(responseCode = "400", description = "Invalid request or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Referenced document or conversation not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<Flux<String>> streamQuestion(@Valid @RequestBody ChatRequestDto request,
			Authentication authentication) {
		logger.info("Received streamQuestion request for userId={}, conversationId={}", requireUser(authentication).getId(),
				request.getConversationId());

		if (request.getConversationId() == null || request.getConversationId().isBlank()) {
			request.setConversationId(UUID.randomUUID().toString());
		}

		Flux<String> stream = ragService.streamQuestionAnswer(request, requireUser(authentication));

		logger.info("streamQuestion initiated for userId={}, conversationId={}", requireUser(authentication).getId(),
				request.getConversationId());
		return ResponseEntity.ok().header("X-Conversation-Id", request.getConversationId())
				.contentType(MediaType.TEXT_EVENT_STREAM).body(stream);
	}

	@Operation(summary = "Search similar document chunks", description = "Performs vector similarity search over indexed document chunks belonging to the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Similarity search completed successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid search request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Referenced document not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@PostMapping("/search/similarity")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<SearchResultDto>> searchSimilar(
			@Valid @RequestBody SearchRequestDto request, Authentication authentication) {

		SearchResultDto result = ragService.searchSimilarChunks(request, requireUser(authentication));

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<SearchResultDto>builder().success(true)
				.data(result).timestamp(LocalDateTime.now()).build());
	}

	@Operation(summary = "Get conversations", description = "Retrieves conversations belonging to the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@GetMapping("/conversations")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<List<ConversationDto>>> getConversations(
			Authentication authentication) {

		List<ConversationDto> conversations = conversationService.getUserConversations(requireUser(authentication));

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<List<ConversationDto>>builder().success(true)
				.message("Conversations retrieved successfully").data(conversations).timestamp(LocalDateTime.now())
				.build());
	}

	@Operation(summary = "Get conversation messages", description = "Retrieves messages for a conversation belonging to the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Conversation messages retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@GetMapping("/conversations/{conversationId}/messages")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<List<ChatMessageDto>>> getConversationMessages(
			@Parameter(description = "Conversation identifier", required = true) @PathVariable String conversationId,
			Authentication authentication) {

		List<ChatMessageDto> messages = conversationService.getConversationMessages(conversationId,
				requireUser(authentication));

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<List<ChatMessageDto>>builder().success(true)
				.message("Conversation messages retrieved successfully").data(messages).timestamp(LocalDateTime.now())
				.build());
	}

	@Operation(summary = "Delete conversation", description = "Deletes a conversation belonging to the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Conversation deleted successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@DeleteMapping("/conversations/{conversationId}")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<Void>> deleteConversation(
			@Parameter(description = "Conversation identifier", required = true) @PathVariable String conversationId,
			Authentication authentication) {

		conversationService.deleteConversation(conversationId, requireUser(authentication));

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<Void>builder().success(true)
				.message("Conversation deleted successfully").data(null).timestamp(LocalDateTime.now()).build());
	}
}