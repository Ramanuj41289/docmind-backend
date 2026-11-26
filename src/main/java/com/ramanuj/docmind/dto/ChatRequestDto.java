
package com.ramanuj.docmind.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ChatRequest", description = "Request for conversational RAG. Controls question answering and retrieval, not document indexing.")
public class ChatRequestDto {

	@NotBlank(message = "Question cannot be empty")
	@Schema(description = "Natural-language question to answer using the user's indexed documents.", example = "What are the key risks described in the report?", requiredMode = Schema.RequiredMode.REQUIRED)
	private String question;

	@Schema(description = "Optional document identifier to restrict retrieval to one document.", example = "8b9f1b72-5b8f-4c4c-9d4d-7d0c8c0e1234")
	private UUID documentId;

	@Min(1)
	@Max(50)
	@Schema(description = "Maximum number of relevant chunks retrieved for this question. Retrieval parameter only.", minimum = "1", maximum = "50", example = "5")
	private Integer topK;

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	@Schema(description = "Minimum similarity threshold for vector retrieval. Retrieval parameter only.", minimum = "0.0", maximum = "1.0", example = "0.70")
	private Double minSimilarity;

	@Schema(description = "Existing conversation identifier. If omitted, a new conversation is created.", example = "conv-7f42d1")
	private String conversationId;
}
