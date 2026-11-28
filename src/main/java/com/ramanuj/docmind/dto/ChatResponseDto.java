
package com.ramanuj.docmind.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(
    name = "ChatResponse",
    description = "Generated answer, conversation reference, response timing, and source citations."
)
public class ChatResponseDto {

    @Schema(
        description = "Generated answer based on the retrieved document context.",
        example = "The report identifies three principal risks..."
    )
    private String answer;

    @Schema(
        description = "Conversation identifier used to persist and retrieve chat history.",
        example = "conv-7f42d1"
    )
    private String conversationId;

    @Schema(
        description = "Source chunks used to ground the generated answer."
    )
    private List<CitationDto> citations;

    @Schema(
        description = "End-to-end response time in milliseconds.",
        example = "842"
    )
    private Long responseTimeMs;

    @Schema(
        description = "Optional runtime evaluation metadata for quality and observability signals."
    )
    private Map<String, Object> evalMetadata;
}
