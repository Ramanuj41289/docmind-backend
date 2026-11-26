
package com.ramanuj.docmind.dto;

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
@Schema(name = "DocumentIndexingResponse", description = "Result of document upload and indexing. Includes the created document ID and chunk count.")
public class DocumentResponseDto {

	@Schema(description = "Identifier assigned to the uploaded document.")
	private UUID id;

	@Schema(description = "Original uploaded filename.", example = "annual-report.pdf")
	private String fileName;

	@Schema(description = "Uploaded file size in bytes.", example = "245760")
	private Long fileSize;

	@Schema(description = "Document indexing lifecycle status. Typical values include PROCESSING, INDEXED and FAILED.", example = "INDEXED")
	private DocumentStatus status;

	@Schema(description = "Number of text chunks created and submitted to the vector store.")
	private Integer chunksCreated;

	@Schema(description = "Human-readable ingestion result.", example = "File indexed successfully")
	private String message;
}
