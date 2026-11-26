
package com.ramanuj.docmind.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ramanuj.docmind.dto.DocumentMetadataDto;
import com.ramanuj.docmind.dto.DocumentResponseDto;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.DocumentProcessingException;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentMetadataService {
	private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataService.class);
	private final DocumentMetadataRepository documentMetadataRepository;
	private final JdbcTemplate jdbcTemplate;
	private final DocumentParserService parserService;
	private final DocumentIngestionService ingestionService;
	private final DocumentMetadataPersistenceService persistenceService;
	private final ModelMapper modelMapper;

	public DocumentResponseDto uploadAndProcess(MultipartFile file, User user) {
		validateUser(user);
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is required and must not be empty");
		}
		return processUpload(file, user, false);
	}

	public List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files, User user) {
		validateUser(user);
		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("At least one file is required");
		}
		List<DocumentResponseDto> results = new ArrayList<>(files.size());
		for (MultipartFile file : files) {
			results.add(processUpload(file, user, true));
		}
		return results;
	}

	private DocumentResponseDto processUpload(MultipartFile file, User user, boolean continueOnFailure) {
		String fileName = file == null || file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
		String contentType = file == null || file.getContentType() == null ? "application/octet-stream" : file.getContentType();
		long fileSize = file == null ? 0L : file.getSize();
		UUID documentId = null;
		try {
			
			// Parse and validate before creating the database record.
			List<Document> parsedDocs = parserService.parse(file);
			
			// Create metadata only after successful parsing.
			documentId = persistenceService.create(fileName, contentType, fileSize, user.getId());
			DocumentMetadata metadata = documentMetadataRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
			int chunks = ingestionService.ingest(metadata, parsedDocs);
			if (chunks <= 0) {
				throw new DocumentProcessingException("No text chunks generated");
			}
			return DocumentResponseDto.builder().id(documentId).fileName(metadata.getFilename()).fileSize(metadata.getFileSize())
					.chunksCreated(chunks).status(metadata.getStatus()).message("Document successfully processed and indexed").build();
		}
		catch (Exception ex) {
			logger.error("Upload processing failed for document {}", documentId, ex);
		
			// Only mark failed if a database record was actually created.
			if (documentId != null) {
				try {
					persistenceService.markFailed(documentId, "Document processing failed");
				}
				catch (Exception statusException) {
					logger.error("Could not update failed status for document {}", documentId, statusException);
				}
			}
			if (!continueOnFailure) {
				if (ex instanceof DocumentProcessingException processingEx) {
					throw processingEx;
				}
				throw new DocumentProcessingException("Failed to process uploaded document", ex);
			}
			return DocumentResponseDto.builder().id(documentId).fileName(fileName).fileSize(fileSize).chunksCreated(0)
					.status(DocumentStatus.FAILED).message("Failed to process document").build();
		}
	}

	@Transactional(readOnly = true)
	public List<DocumentMetadataDto> getAllDocuments(User user) {
		validateUser(user);
		return documentMetadataRepository.findByUserOrderByCreatedAtDesc(user).stream()
				.map(doc -> modelMapper.map(doc, DocumentMetadataDto.class)).toList();
	}

	@Transactional(readOnly = true)
	public DocumentMetadataDto getDocumentById(UUID id, User user) {
		validateUser(user);
		DocumentMetadata metadata = documentMetadataRepository.findByIdAndUser(id, user)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
		return modelMapper.map(metadata, DocumentMetadataDto.class);
	}

	@Transactional
	public void deleteDocument(UUID id, User user) {
		validateUser(user);
		DocumentMetadata metadata = documentMetadataRepository.findByIdAndUser(id, user)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
		int deletedCount = jdbcTemplate.update("""
				DELETE FROM vector_store
				WHERE metadata->>'documentId' = ?
				""", id.toString());
		logger.info("Deleted {} vector chunks for document {}", deletedCount, id);
		documentMetadataRepository.delete(metadata);
	}

	private void validateUser(User user) {
		if (user == null || user.getId() == null) {
			throw new IllegalArgumentException("Authenticated user is required");
		}
	}
}
