
package com.ramanuj.docmind.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ramanuj.docmind.dto.ApiErrorResponse;
import com.ramanuj.docmind.dto.DocumentMetadataDto;
import com.ramanuj.docmind.dto.DocumentResponseDto;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.service.DocumentMetadataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
	private final DocumentMetadataService documentService;

	private User requireUser(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof User user) {
			return user;
		}
		throw new AccessDeniedException("Authenticated user is required");
	}

	@Operation(summary = "Upload a document", description = "Uploads and processes a single document for the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Document uploaded and processed successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "ValidationError", value = """
					{
					  "success": false,
					  "message": "Validation failed",
					  "data": {
					    "file": "File is required"
					  },
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AuthenticationError", value = """
					{
					  "success": false,
					  "message": "Invalid username or password",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}"""))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AccessDenied", value = """
					{
					  "success": false,
					  "message": "You do not have permission to access this resource",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}"""))),
			@ApiResponse(responseCode = "413", description = "Uploaded file exceeds the maximum allowed size", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "FileTooLarge", value = """
					{
					  "success": false,
					  "message": "File size exceeds the allowed limit of 25MB",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}"""))),
			@ApiResponse(responseCode = "422", description = "Document processing failed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "ProcessingError", value = """
					{
					  "success": false,
					  "message": "Failed to process document",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "InternalServerError", value = """
					{
					  "success": false,
					  "message": "An unexpected error occurred",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))) })
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<DocumentResponseDto>> upload(
			@Parameter(description = "Document file to upload", required = true) @RequestParam("file") MultipartFile file,
			Authentication authentication) {

		DocumentResponseDto result = documentService.uploadAndProcess(file, requireUser(authentication));

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(com.ramanuj.docmind.dto.ApiResponse.<DocumentResponseDto>builder().success(true).data(result)
						.timestamp(LocalDateTime.now()).message("File indexed successfully").build());
	}

	@Operation(summary = "Upload multiple documents", description = "Uploads and processes multiple documents for the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Batch upload completed"),
			@ApiResponse(responseCode = "400", description = "Invalid request or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "ValidationError", value = """
					{
					  "success": false,
					  "message": "Validation failed",
					  "data": {
					    "files": "At least one file is required"
					  },
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AuthenticationError", value = """
					{
					  "success": false,
					  "message": "Invalid username or password",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AccessDenied", value = """
					{
					  "success": false,
					  "message": "You do not have permission to access this resource",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "413", description = "Uploaded file exceeds the maximum allowed size", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "FileTooLarge", value = """
					{
					  "success": false,
					  "message": "File size exceeds the allowed limit of 25MB",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "422", description = "Document processing failed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "ProcessingError", value = """
					{
					  "success": false,
					  "message": "Failed to process document",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "InternalServerError", value = """
					{
					  "success": false,
					  "message": "An unexpected error occurred",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))) })
	@PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<List<DocumentResponseDto>>> uploadMultiple(
			@Parameter(description = "Document files to upload", required = true) @RequestParam("files") List<MultipartFile> files,
			Authentication authentication) {
		List<DocumentResponseDto> results = documentService.uploadMultipleDocuments(files, requireUser(authentication));
		long successCount = results.stream().filter(response -> response.getStatus() == DocumentStatus.INDEXED).count();
		long failedCount = results.stream().filter(response -> response.getStatus() == DocumentStatus.FAILED).count();
		String message;
		if (successCount == results.size()) {
			message = "All documents uploaded and indexed successfully.";
		} else if (failedCount == results.size()) {
			message = "Batch upload failed. No documents were indexed.";
		} else {
			message = "Batch upload completed with partial failures.";
		}
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(com.ramanuj.docmind.dto.ApiResponse.<List<DocumentResponseDto>>builder().success(true)
						.data(results).timestamp(LocalDateTime.now()).message(message).build());
	}

	@Operation(summary = "Get all uploaded documents", description = "Retrieves document metadata belonging to the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AuthenticationError", value = """
					{
					  "success": false,
					  "message": "Invalid username or password",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AccessDenied", value = """
					{
					  "success": false,
					  "message": "You do not have permission to access this resource",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "InternalServerError", value = """
					{
					  "success": false,
					  "message": "An unexpected error occurred",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))) })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<List<DocumentMetadataDto>>> getAll(
			Authentication authentication) {
		List<DocumentMetadataDto> results = documentService.getAllDocuments(requireUser(authentication));
		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<List<DocumentMetadataDto>>builder().success(true)
				.data(results).timestamp(LocalDateTime.now()).message("Documents retrieved successfully").build());
	}

	@Operation(summary = "Get document by ID", description = "Retrieves metadata for a specific document belonging to the authenticated user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Document retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AuthenticationError", value = """
					{
					  "success": false,
					  "message": "Invalid username or password",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AccessDenied", value = """
					{
					  "success": false,
					  "message": "You do not have permission to access this resource",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "404", description = "Document not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "DocumentNotFound", value = """
					{
					  "success": false,
					  "message": "Document not found",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "InternalServerError", value = """
					{
					  "success": false,
					  "message": "An unexpected error occurred",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))) })
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<DocumentMetadataDto>> getById(
			@Parameter(description = "Document UUID", required = true) @PathVariable UUID id,
			Authentication authentication) {
		DocumentMetadataDto result = documentService.getDocumentById(id, requireUser(authentication));
		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<DocumentMetadataDto>builder().success(true)
				.data(result).timestamp(LocalDateTime.now()).message("Document retrieved successfully").build());
	}

	@Operation(summary = "Delete a document", description = "Deletes a document belonging to the authenticated user.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Document deleted successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(ref = "#/components/schemas/ApiResponseVoid"))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AuthenticationError", value = """
					{
					  "success": false,
					  "message": "Invalid username or password",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "AccessDenied", value = """
					{
					  "success": false,
					  "message": "You do not have permission to access this resource",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "404", description = "Document not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "DocumentNotFound", value = """
					{
					  "success": false,
					  "message": "Document not found",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "InternalServerError", value = """
					{
					  "success": false,
					  "message": "An unexpected error occurred",
					  "data": null,
					  "timestamp": "2026-11-26T16:30:00"
					}
					"""))) })
	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<Void>> delete(
			@Parameter(description = "Document UUID", required = true) @PathVariable UUID id,
			Authentication authentication) {
		documentService.deleteDocument(id, requireUser(authentication));
		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<Void>builder().success(true).data(null)
				.timestamp(LocalDateTime.now()).message("Document deleted successfully").build());
	}
}
