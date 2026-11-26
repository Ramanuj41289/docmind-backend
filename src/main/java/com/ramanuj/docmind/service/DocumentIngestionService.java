
package com.ramanuj.docmind.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.exception.DocumentProcessingException;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {
	private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);
	private final VectorStore vectorStore;
	private final DocumentMetadataRepository documentMetadataRepository;
	private final AppProperties appProperties;
	@Transactional
	public int ingest(DocumentMetadata metadata, List<Document> parsedDocs) {
		if (metadata == null || metadata.getId() == null) {
			throw new IllegalArgumentException("Document metadata is required");
		}
		if (parsedDocs == null || parsedDocs.isEmpty()) {
			throw new DocumentProcessingException("No readable content found");
		}
		if (metadata.getUser() == null || metadata.getUser().getId() == null) {
			throw new DocumentProcessingException("Document owner is missing");
		}
		UUID documentId = metadata.getId();
		try {
			metadata.setStatus(DocumentStatus.PROCESSING);
			metadata.setTotalPages(parsedDocs.size());
			TokenTextSplitter splitter = TokenTextSplitter.builder()
					.withChunkSize(appProperties.getRag().getChunkSize())
					.withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChars())
					.withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
					.withMaxNumChunks(appProperties.getRag().getMaxNumChunks()).withKeepSeparator(true).build();
			List<Document> chunks = splitter.apply(parsedDocs);
			if (chunks.isEmpty()) {
				throw new DocumentProcessingException("Document appears to be empty or unscannable");
			}
			List<Document> enrichedChunks = new ArrayList<>(chunks.size());
			for (int i = 0; i < chunks.size(); i++) {
				Document chunk = chunks.get(i);
				Map<String, Object> metadataMap = new HashMap<>(chunk.getMetadata());
				metadataMap.put("userId", metadata.getUser().getId().toString());
				metadataMap.put("documentId", documentId.toString());
				metadataMap.put("fileName", metadata.getFilename());
				metadataMap.put("contentType", metadata.getContentType());
				metadataMap.put("chunkIndex", i);
				Object pageNumber = metadataMap.get("page_number");
				if (pageNumber == null) {
					pageNumber = metadataMap.get("pageNumber");
				}
				if (pageNumber != null) {
					metadataMap.put("pageNumber", pageNumber);
				}
				enrichedChunks.add(new Document(chunk.getText(), metadataMap));
			}
			vectorStore.add(enrichedChunks);
			metadata.setStatus(DocumentStatus.INDEXED);
			metadata.setTotalChunks(enrichedChunks.size());
			metadata.setErrorMessage(null);
			metadata.setUpdatedAt(LocalDateTime.now());
			documentMetadataRepository.save(metadata);
			logger.info("Indexed document id={} userId={} chunks={}", documentId, metadata.getUser().getId(), enrichedChunks.size());
			return enrichedChunks.size();
		}
		catch (DocumentProcessingException ex) {
			throw ex;
		}
		catch (Exception ex) {
			logger.error("Document ingestion failed: {}", documentId, ex);
			throw new DocumentProcessingException("Failed to index document", ex);
		}
	}
}
