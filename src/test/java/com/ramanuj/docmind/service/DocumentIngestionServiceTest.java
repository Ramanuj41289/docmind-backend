
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.DocumentProcessingException;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

	@Mock
	private VectorStore vectorStore;

	@Mock
	private DocumentMetadataRepository documentMetadataRepository;

	@Captor
	private ArgumentCaptor<List<Document>> documentsCaptor;

	@InjectMocks
	private DocumentIngestionService documentIngestionService;

	private AppProperties appProperties;

	private User user;

	private DocumentMetadata metadata;

	@BeforeEach
	void setUp() {
		appProperties = new AppProperties();

		appProperties.getRag().setChunkSize(700);
		appProperties.getRag().setMinChunkSizeChars(100);
		appProperties.getRag().setMinChunkLengthToEmbed(5);
		appProperties.getRag().setMaxNumChunks(100);

		documentIngestionService = new DocumentIngestionService(vectorStore, documentMetadataRepository, appProperties);

		user = User.builder().id(1L).username("testuser").email("test@example.com").build();

		metadata = DocumentMetadata.builder().id(UUID.randomUUID()).filename("test-document.txt")
				.contentType("text/plain").fileSize(1024L).status(DocumentStatus.UPLOADING).user(user).build();
	}

	@Test
	void ingest_shouldRejectMetadataWithoutUser() {

		metadata.setUser(null);

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, List.of(new Document("Some document content"))));

		assertEquals("Document owner is missing", exception.getMessage());

		verifyNoInteractions(vectorStore);
		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void ingest_shouldRejectUserWithoutId() {

		user.setId(null);

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, List.of(new Document("Some document content"))));

		assertEquals("Document owner is missing", exception.getMessage());

		verifyNoInteractions(vectorStore);
		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void ingest_shouldRejectNullParsedDocuments() {

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, null));

		assertEquals("No readable content found", exception.getMessage());

		verifyNoInteractions(vectorStore);
		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void ingest_shouldRejectEmptyParsedDocuments() {

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, List.of()));

		assertEquals("No readable content found", exception.getMessage());

		verifyNoInteractions(vectorStore);
		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void ingest_shouldSuccessfullyIndexDocument() {

		List<Document> parsedDocuments = List
				.of(new Document("This is a test document containing enough readable content for indexing."));

		int chunksCreated = documentIngestionService.ingest(metadata, parsedDocuments);

		assertTrue(chunksCreated > 0);
		assertEquals(DocumentStatus.INDEXED, metadata.getStatus());
		assertEquals(1, metadata.getTotalPages());
		assertEquals(chunksCreated, metadata.getTotalChunks());
		assertNull(metadata.getErrorMessage());
		assertNotNull(metadata.getUpdatedAt());

		verify(vectorStore).add(anyList());
		verify(documentMetadataRepository).save(metadata);
	}

	@Test
	void ingest_shouldAddOwnershipMetadataToChunks() {

		List<Document> parsedDocuments = List.of(new Document(
				"This is a document with enough content to generate an embedding chunk.", Map.of("source", "test")));

		documentIngestionService.ingest(metadata, parsedDocuments);

		verify(vectorStore).add(documentsCaptor.capture());

		List<Document> chunks = documentsCaptor.getValue();

		assertFalse(chunks.isEmpty());

		for (Document chunk : chunks) {
			Map<String, Object> chunkMetadata = chunk.getMetadata();

			assertEquals("1", chunkMetadata.get("userId"));
			assertEquals(metadata.getId().toString(), chunkMetadata.get("documentId"));
			assertEquals("test-document.txt", chunkMetadata.get("fileName"));
			assertEquals("text/plain", chunkMetadata.get("contentType"));
			assertNotNull(chunkMetadata.get("chunkIndex"));
		}
	}

	@Test
	void ingest_shouldPreserveOriginalDocumentMetadata() {

		List<Document> parsedDocuments = List.of(new Document(
				"This is a document containing enough content for indexing.", Map.of("source", "original-source")));

		documentIngestionService.ingest(metadata, parsedDocuments);

		verify(vectorStore).add(documentsCaptor.capture());

		Document chunk = documentsCaptor.getValue().get(0);

		assertEquals("original-source", chunk.getMetadata().get("source"));
	}

	@Test
	void ingest_shouldNormalizePageNumberMetadata() {

		List<Document> parsedDocuments = List
				.of(new Document("This document contains enough text for indexing.", Map.of("page_number", 3)));

		documentIngestionService.ingest(metadata, parsedDocuments);

		verify(vectorStore).add(documentsCaptor.capture());

		Document chunk = documentsCaptor.getValue().get(0);

		assertEquals(3, chunk.getMetadata().get("pageNumber"));
	}

	@Test
	void ingest_shouldUseExistingPageNumberWhenPageNumberKeyIsAbsent() {

		List<Document> parsedDocuments = List
				.of(new Document("This document contains enough text for indexing.", Map.of("pageNumber", 5)));

		documentIngestionService.ingest(metadata, parsedDocuments);

		verify(vectorStore).add(documentsCaptor.capture());

		Document chunk = documentsCaptor.getValue().get(0);

		assertEquals(5, chunk.getMetadata().get("pageNumber"));
	}

	@Test
	void ingest_shouldMarkDocumentAsFailedWhenVectorStoreFails() {

		List<Document> parsedDocuments = List.of(new Document("This is sufficient content for document indexing."));

		doThrow(new RuntimeException("Vector store unavailable")).when(vectorStore).add(anyList());

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, parsedDocuments));

		assertEquals("Failed to index document", exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals("Vector store unavailable", exception.getCause().getMessage());

		assertEquals(DocumentStatus.FAILED, metadata.getStatus());
		assertEquals("Document indexing failed", metadata.getErrorMessage());
		assertNotNull(metadata.getUpdatedAt());

		verify(documentMetadataRepository).save(metadata);
	}

	@Test
	void ingest_shouldNotSaveMetadataWhenOwnerValidationFails() {

		metadata.setUser(null);

		assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, List.of(new Document("Valid document content"))));

		verify(documentMetadataRepository, never()).save(any());
	}

	@Test
	void ingest_shouldNotSaveMetadataWhenParsedDocumentsAreEmpty() {

		assertThrows(DocumentProcessingException.class, () -> documentIngestionService.ingest(metadata, List.of()));

		verify(documentMetadataRepository, never()).save(any());
	}

	@Test
	void ingest_shouldRejectDocumentsWithoutReadableText() {

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentIngestionService.ingest(metadata, List.of(new Document(""))));

		assertEquals("Document appears to be empty or unscannable", exception.getMessage());

		assertEquals(DocumentStatus.PROCESSING, metadata.getStatus());

		verifyNoInteractions(vectorStore);
		verify(documentMetadataRepository, never()).save(any());
	}

	@Test
	void ingest_shouldSetTotalPagesForMultipleParsedDocuments() {

		List<Document> parsedDocuments = List.of(
				new Document("This is the first page with sufficient content for indexing."),
				new Document("This is the second page with sufficient content for indexing."));

		int chunksCreated = documentIngestionService.ingest(metadata, parsedDocuments);

		assertTrue(chunksCreated > 0);
		assertEquals(2, metadata.getTotalPages());
		assertEquals(chunksCreated, metadata.getTotalChunks());
		assertEquals(DocumentStatus.INDEXED, metadata.getStatus());

		verify(vectorStore).add(anyList());
		verify(documentMetadataRepository).save(metadata);
	}

	@Test
	void ingest_shouldNotAddPageNumberWhenPageMetadataIsMissing() {

		List<Document> parsedDocuments = List.of(new Document("This document has sufficient content for indexing."));

		documentIngestionService.ingest(metadata, parsedDocuments);

		verify(vectorStore).add(documentsCaptor.capture());

		Document chunk = documentsCaptor.getValue().get(0);

		assertFalse(chunk.getMetadata().containsKey("pageNumber"));
	}
}
