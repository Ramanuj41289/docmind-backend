
package com.ramanuj.docmind.dto;

import java.util.List;

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
@Schema(name = "SimilaritySearchResult", description = "Vector retrieval result containing matching citation chunks.")
public class SearchResultDto {

	@Schema(description = "Original retrieval query.", example = "What is the investment strategy?")
	private String query;

	@Schema(description = "Number of chunks returned by vector similarity search.", example = "5")
	private int totalMatches;

	@Schema(description = "Retrieved chunks, represented as citation objects with document and chunk metadata.")
	private List<CitationDto> matches;
}
