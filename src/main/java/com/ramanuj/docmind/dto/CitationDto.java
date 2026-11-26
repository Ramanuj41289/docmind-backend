
package com.ramanuj.docmind.dto;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "Citation", description = "A retrieved source chunk and its document, page, similarity, and metadata details.")
public class CitationDto {

	@Schema(description = "Identifier of the source document.")
	private UUID documentId;

	@Schema(description = "Original uploaded filename.", example = "annual-report.pdf")
	private String fileName;

	@Schema(description = "Zero-based index of the indexed text chunk.", example = "3")
	private Integer chunkIndex;

	@Schema(description = "Source page number when supplied by the document parser.", example = "12")
	private Integer pageNumber;

	@Schema(description = "Retrieved text excerpt used as evidence.")
	private String snippet;

	@Schema(description = "Similarity score derived from the vector-store distance metadata, when available.", example = "0.87")
	private Double similarityScore;

	@Schema(description = "Additional source metadata stored with the indexed vector chunk.")
	private Map<String, Object> metaData;
}
