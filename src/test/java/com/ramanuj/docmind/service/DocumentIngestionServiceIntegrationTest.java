
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.DocumentProcessingException;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

@DataJpaTest
@Import({ DocumentIngestionService.class, AppProperties.class })
class DocumentIngestionServiceIntegrationTest {

	@Autowired
	private DocumentIngestionService documentIngestionService;

	@Autowired
	private DocumentMetadataRepository documentMetadataRepository;

	@Autowired
	private TestEntityManager entityManager;

	@MockitoBean
	private VectorStore vectorStore;

	@Test
	void ingest_shouldPersistIndexedDocumentAndEnrichVectorMetadata() {
		User user = createUser("ingestionuser");

		DocumentMetadata metadata = createDocumentMetadata(user, "integration-test.pdf");

		documentMetadataRepository.saveAndFlush(metadata);

		String content = createLongDocumentContent();

		Document parsedDocument = new Document(content, Map.of("page_number", 1));

		AtomicReference<List<Document>> capturedChunks = new AtomicReference<>();

		doAnswer(invocation -> {
			List<Document> chunks = invocation.getArgument(0);
			capturedChunks.set(new ArrayList<>(chunks));
			return null;
		}).when(vectorStore).add(anyList());

		int chunkCount = documentIngestionService.ingest(metadata, List.of(parsedDocument));

		entityManager.flush();
		entityManager.clear();

		DocumentMetadata persisted = documentMetadataRepository.findById(metadata.getId()).orElseThrow();

		assertThat(chunkCount).isPositive();
		assertThat(persisted.getStatus()).isEqualTo(DocumentStatus.INDEXED);
		assertThat(persisted.getTotalPages()).isEqualTo(1);
		assertThat(persisted.getTotalChunks()).isEqualTo(chunkCount);
		assertThat(persisted.getErrorMessage()).isNull();
		assertThat(persisted.getUpdatedAt()).isNotNull();

		assertThat(capturedChunks.get()).isNotNull();
		assertThat(capturedChunks.get()).hasSize(chunkCount);

		Document firstChunk = capturedChunks.get().get(0);

		assertThat(firstChunk.getMetadata()).containsEntry("userId", user.getId().toString())
				.containsEntry("documentId", metadata.getId().toString())
				.containsEntry("fileName", "integration-test.pdf").containsEntry("contentType", "application/pdf")
				.containsEntry("chunkIndex", 0).containsEntry("pageNumber", 1);

		verify(vectorStore).add(anyList());
	}

	@Test
	void ingest_shouldRejectNullParsedDocuments() {
		User user = createUser("nullcontentuser");

		DocumentMetadata metadata = createDocumentMetadata(user, "null-content.pdf");

		assertThatThrownBy(() -> documentIngestionService.ingest(metadata, null))
				.isInstanceOf(DocumentProcessingException.class).hasMessage("No readable content found");

		verify(vectorStore, never()).add(anyList());
	}

	@Test
	void ingest_shouldRejectEmptyParsedDocuments() {
		User user = createUser("emptycontentuser");

		DocumentMetadata metadata = createDocumentMetadata(user, "empty-content.pdf");

		assertThatThrownBy(() -> documentIngestionService.ingest(metadata, List.of()))
				.isInstanceOf(DocumentProcessingException.class).hasMessage("No readable content found");

		verify(vectorStore, never()).add(anyList());
	}

	@Test
	void ingest_shouldRejectDocumentWithoutOwner() {
		DocumentMetadata metadata = DocumentMetadata.builder().filename("owner-missing.pdf")
				.contentType("application/pdf").fileSize(1024L).status(DocumentStatus.UPLOADING)
				.createdAt(LocalDateTime.now()).build();

		Document parsedDocument = new Document(createLongDocumentContent());

		assertThatThrownBy(() -> documentIngestionService.ingest(metadata, List.of(parsedDocument)))
				.isInstanceOf(DocumentProcessingException.class).hasMessage("Document owner is missing");

		verify(vectorStore, never()).add(anyList());
	}

	@Test
	void ingest_shouldRejectOwnerWithoutDatabaseId() {
		User user = User.builder().username("transientuser").password("encoded-password")
				.email("transientuser@test.com").role(Role.USER).build();

		DocumentMetadata metadata = createDocumentMetadata(user, "transient-owner.pdf");

		Document parsedDocument = new Document(createLongDocumentContent());

		assertThatThrownBy(() -> documentIngestionService.ingest(metadata, List.of(parsedDocument)))
				.isInstanceOf(DocumentProcessingException.class).hasMessage("Document owner is missing");

		verify(vectorStore, never()).add(anyList());
	}

	private User createUser(String username) {
		User user = User.builder().username(username).password("encoded-password").email(username + "@test.com")
				.role(Role.USER).build();

		return entityManager.persistAndFlush(user);
	}

	private DocumentMetadata createDocumentMetadata(User user, String filename) {

		return DocumentMetadata.builder().filename(filename).contentType("application/pdf").fileSize(2048L)
				.status(DocumentStatus.UPLOADING).createdAt(LocalDateTime.now()).user(user).build();
	}

	private String createLongDocumentContent() {
		return String.join(" ",
				java.util.Collections.nCopies(500, "DocMind integration testing verifies document ingestion, "
						+ "metadata enrichment, ownership isolation, " + "and successful vector indexing."));
	}
}
