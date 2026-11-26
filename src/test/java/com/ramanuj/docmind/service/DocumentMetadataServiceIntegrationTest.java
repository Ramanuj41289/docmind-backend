
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.ai.document.Document;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ramanuj.docmind.dto.DocumentMetadataDto;
import com.ramanuj.docmind.dto.DocumentResponseDto;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.DocumentProcessingException;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

@DataJpaTest
@Import({ DocumentMetadataService.class, DocumentMetadataServiceIntegrationTest.TestConfig.class })
class DocumentMetadataServiceIntegrationTest {

	@TestConfiguration
	static class TestConfig {

		@Bean
		ModelMapper modelMapper() {
			return new ModelMapper();
		}
	}

	@jakarta.annotation.Resource
	private DocumentMetadataService documentMetadataService;

	@jakarta.annotation.Resource
	private DocumentMetadataRepository documentMetadataRepository;

	@jakarta.annotation.Resource
	private TestEntityManager entityManager;

	@MockitoBean
	private DocumentParserService parserService;

	@MockitoBean
	private DocumentIngestionService ingestionService;

	@MockitoBean
	private JdbcTemplate jdbcTemplate;

	private User user;
	private User anotherUser;

	@BeforeEach
	void setUp() {
		user = createUser("metadatauser");
		anotherUser = createUser("anotheruser");
	}

	@Test
	void uploadAndProcess_shouldPersistDocumentMetadata() {

		MockMultipartFile file = createFile("integration.pdf");

		when(parserService.parse(file)).thenReturn(List.of(new Document("Document integration content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenAnswer(invocation -> {
			DocumentMetadata metadata = invocation.getArgument(0);
			metadata.setStatus(DocumentStatus.INDEXED);
			metadata.setTotalPages(1);
			metadata.setTotalChunks(3);
			return 3;
		});

		DocumentResponseDto response = documentMetadataService.uploadAndProcess(file, user);

		entityManager.flush();
		entityManager.clear();

		DocumentMetadata persisted = documentMetadataRepository.findById(response.getId()).orElseThrow();

		assertThat(response.getId()).isNotNull();
		assertThat(response.getFileName()).isEqualTo("integration.pdf");
		assertThat(response.getChunksCreated()).isEqualTo(3);
		assertThat(response.getStatus()).isEqualTo(DocumentStatus.INDEXED);

		assertThat(persisted.getFilename()).isEqualTo("integration.pdf");
		assertThat(persisted.getContentType()).isEqualTo("application/pdf");
		assertThat(persisted.getFileSize()).isEqualTo(file.getSize());
		assertThat(persisted.getStatus()).isEqualTo(DocumentStatus.INDEXED);
		assertThat(persisted.getUser().getId()).isEqualTo(user.getId());

		verify(parserService).parse(file);
		verify(ingestionService).ingest(any(DocumentMetadata.class), anyList());
	}

	@Test
	void uploadMultipleDocuments_shouldPersistAllDocuments() {

		MockMultipartFile firstFile = createFile("first.pdf");
		MockMultipartFile secondFile = createFile("second.pdf");

		when(parserService.parse(any())).thenReturn(List.of(new Document("Document content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenAnswer(invocation -> {
			DocumentMetadata metadata = invocation.getArgument(0);
			metadata.setStatus(DocumentStatus.INDEXED);
			return 2;
		});

		List<DocumentResponseDto> responses = documentMetadataService
				.uploadMultipleDocuments(List.of(firstFile, secondFile), user);

		entityManager.flush();
		entityManager.clear();

		assertThat(responses).hasSize(2);
		assertThat(documentMetadataRepository.findByUserOrderByCreatedAtDesc(user)).hasSize(2);

		assertThat(responses.get(0).getFileName()).isEqualTo("first.pdf");
		assertThat(responses.get(1).getFileName()).isEqualTo("second.pdf");

		verify(parserService, org.mockito.Mockito.times(2)).parse(any());
		verify(ingestionService, org.mockito.Mockito.times(2)).ingest(any(DocumentMetadata.class), anyList());
	}

	@Test
	void getAllDocuments_shouldReturnOnlyAuthenticatedUsersDocuments() {

		createMetadata(user, "user-document.pdf");
		createMetadata(anotherUser, "other-document.pdf");

		entityManager.flush();
		entityManager.clear();

		List<DocumentMetadataDto> documents = documentMetadataService.getAllDocuments(user);

		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getFilename()).isEqualTo("user-document.pdf");
	}

	@Test
	void getDocumentById_shouldReturnOwnedDocument() {

		DocumentMetadata metadata = createMetadata(user, "owned-document.pdf");

		entityManager.flush();
		entityManager.clear();

		DocumentMetadataDto result = documentMetadataService.getDocumentById(metadata.getId(), user);

		assertThat(result.getId()).isEqualTo(metadata.getId());
		assertThat(result.getFilename()).isEqualTo("owned-document.pdf");
		assertThat(result.getStatus()).isEqualTo(DocumentStatus.INDEXED);
	}

	@Test
	void getDocumentById_shouldRejectAnotherUsersDocument() {

		DocumentMetadata metadata = createMetadata(anotherUser, "private-document.pdf");

		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> documentMetadataService.getDocumentById(metadata.getId(), user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found");
	}

	@Test
	void getDocumentById_shouldRejectUnknownDocument() {

		UUID unknownId = UUID.randomUUID();

		assertThatThrownBy(() -> documentMetadataService.getDocumentById(unknownId, user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found");
	}

	@Test
	void deleteDocument_shouldDeleteVectorsAndMetadata() {

		DocumentMetadata metadata = createMetadata(user, "delete-document.pdf");

		entityManager.flush();
		entityManager.clear();

		UUID documentId = metadata.getId();

		when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(3);

		documentMetadataService.deleteDocument(documentId, user);

		entityManager.flush();
		entityManager.clear();

		assertThat(documentMetadataRepository.findById(documentId)).isEmpty();

		verify(jdbcTemplate).update(contains("metadata->>'documentId'"), any(Object[].class));
	}

	@Test
	void deleteDocument_shouldRejectAnotherUsersDocument() {

		DocumentMetadata metadata = createMetadata(anotherUser, "protected-document.pdf");

		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> documentMetadataService.deleteDocument(metadata.getId(), user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found");

		verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));

		assertThat(documentMetadataRepository.findById(metadata.getId())).isPresent();
	}

	@Test
	void deleteDocument_shouldRejectUnknownDocument() {

		assertThatThrownBy(() -> documentMetadataService.deleteDocument(UUID.randomUUID(), user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found");

		verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
	}

	@Test
	void getAllDocuments_shouldReturnEmptyListWhenUserHasNoDocuments() {

		List<DocumentMetadataDto> documents = documentMetadataService.getAllDocuments(user);

		assertThat(documents).isEmpty();
	}

	@Test
	void uploadAndProcess_shouldRejectProcessingWhenParserFails() {

		MockMultipartFile file = createFile("parser-failure.pdf");

		when(parserService.parse(file)).thenThrow(new DocumentProcessingException("Parsing failed"));

		assertThatThrownBy(() -> documentMetadataService.uploadAndProcess(file, user))
				.isInstanceOf(DocumentProcessingException.class).hasMessage("Parsing failed");

		verify(ingestionService, never()).ingest(any(DocumentMetadata.class), anyList());
	}

	@Test
	void uploadAndProcess_shouldRejectZeroGeneratedChunks() {

		MockMultipartFile file = createFile("zero-chunks.pdf");

		when(parserService.parse(file)).thenReturn(List.of(new Document("Document content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenReturn(0);

		assertThatThrownBy(() -> documentMetadataService.uploadAndProcess(file, user))
				.isInstanceOf(DocumentProcessingException.class).hasMessage("No text chunks generated");
	}

	@Test
	void uploadAndProcess_shouldRejectNullUser() {

		MockMultipartFile file = createFile("invalid-user.pdf");

		assertThatThrownBy(() -> documentMetadataService.uploadAndProcess(file, null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");

		verify(parserService, never()).parse(any());
	}

	@Test
	void uploadMultipleDocuments_shouldRejectEmptyFileList() {

		assertThatThrownBy(() -> documentMetadataService.uploadMultipleDocuments(List.of(), user))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("At least one file is required");
	}

	private User createUser(String username) {

		User newUser = User.builder().username(username).email(username + "@test.com").password("encoded-password")
				.role(Role.USER).build();

		return entityManager.persistAndFlush(newUser);
	}

	private DocumentMetadata createMetadata(User owner, String filename) {

		DocumentMetadata metadata = DocumentMetadata.builder().filename(filename).contentType("application/pdf")
				.fileSize(2048L).totalPages(2).totalChunks(3).status(DocumentStatus.INDEXED)
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).user(owner).build();

		return entityManager.persistAndFlush(metadata);
	}

	private MockMultipartFile createFile(String filename) {

		return new MockMultipartFile("file", filename, "application/pdf",
				"Sample document content for integration testing".getBytes());
	}
}
