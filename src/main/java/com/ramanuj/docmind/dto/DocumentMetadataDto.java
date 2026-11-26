
package com.ramanuj.docmind.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ramanuj.docmind.entity.DocumentStatus;

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
@Schema(name = "DocumentMetadata", description = "Document metadata and indexing lifecycle information. Does not expose retrieval settings.")
public class DocumentMetadataDto {

	@Schema(description = "Document identifier.")
	private UUID id;

	@Schema(description = "Original uploaded filename.", example = "annual-report.pdf")
	private String filename;

	@Schema(description = "Detected or supplied MIME content type.", example = "application/pdf")
	private String contentType;

	@Schema(description = "File size in bytes.", example = "245760")
	private Long fileSize;

	@Schema(description = "Number of parsed document units/pages.", example = "18")
	private Integer totalPages;

	@Schema(description = "Number of chunks indexed in the vector store.", example = "42")
	private Integer totalChunks;

	@Schema(description = "Document indexing status.", example = "INDEXED")
	private DocumentStatus status;

	@Schema(description = "Processing error message when indexing fails; otherwise null.")
	private String errorMessage;

	@Schema(description = "Document record creation timestamp.", example = "2026-11-25T14:30:00")
	private LocalDateTime createdAt;

	@Schema(description = "Most recent document record update timestamp.", example = "2026-11-25T14:32:00")
	private LocalDateTime updatedAt;
}
