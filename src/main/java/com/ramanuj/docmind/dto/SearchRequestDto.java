
package com.ramanuj.docmind.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "SimilaritySearchRequest", description = "Request for standalone vector similarity retrieval over indexed document chunks.")
public class SearchRequestDto {

	@NotBlank(message = "Query cannot be empty")
	@Schema(description = "Text query used to find semantically similar indexed document chunks.", example = "Explain the fund's liquidity risk.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String query;

	@Schema(description = "Optional document identifier. If omitted, searches all indexed documents owned by the authenticated user.", example = "8b9f1b72-5b8f-4c4c-9d4d-7d0c8c0e1234")
	private UUID documentId;

	@Schema(description = "Maximum number of similar chunks returned by retrieval. This parameter does not control document indexing or chunk creation.", minimum = "1", maximum = "50", example = "10")
	private Integer topK;

	@Schema(description = "Minimum similarity threshold for retrieval. Null or non-positive values use the configured default.", minimum = "0.0", maximum = "1.0", example = "0.70")
	private Double similaritySearch;
}
