
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ramanuj.docmind.dto.DocumentMetadataDto;
import com.ramanuj.docmind.dto.DocumentResponseDto;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.DocumentProcessingException;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataServiceTest {

	@Mock
	private DocumentMetadataRepository documentMetadataRepository;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private DocumentParserService parserService;

	@Mock
	private DocumentIngestionService ingestionService;

	@Mock
	private ModelMapper modelMapper;

	@Mock
	private DocumentMetadataPersistenceService persistenceService;

	@InjectMocks
	private DocumentMetadataService documentMetadataService;

	private User user;

	@BeforeEach
	void setUp() {

		user = User.builder().id(1L).username("testuser").email("test@example.com").password("encoded-password")
				.role(Role.USER).build();

		lenient().when(persistenceService.create(anyString(), anyString(), any(Long.class), any(Long.class)))
				.thenAnswer(invocation -> {
					UUID id = UUID.randomUUID();
					DocumentMetadata meta = DocumentMetadata.builder().id(id).filename(invocation.getArgument(0))
							.contentType(invocation.getArgument(1)).fileSize(invocation.getArgument(2))
							.status(DocumentStatus.UPLOADING).user(user).build();
					lenient().when(documentMetadataRepository.findById(id)).thenReturn(Optional.of(meta));
					return id;
				});

		lenient().when(documentMetadataRepository.save(any(DocumentMetadata.class))).thenAnswer(invocation -> {
			DocumentMetadata metadata = invocation.getArgument(0);

			if (metadata.getId() == null) {
				metadata.setId(UUID.randomUUID());
			}

			return metadata;
		});
	}

	private MockMultipartFile createFile(String filename, String content) {
		return new MockMultipartFile("file", filename, "text/plain", content.getBytes());
	}

	private DocumentMetadata createMetadata(UUID id) {
		return DocumentMetadata.builder().id(id).filename("test.txt").contentType("text/plain").fileSize(100L)
				.totalPages(1).totalChunks(2).status(DocumentStatus.INDEXED).user(user).build();
	}

	private DocumentMetadataDto createMetadataDto(UUID id) {
		return DocumentMetadataDto.builder().id(id).filename("test.txt").contentType("text/plain").fileSize(100L)
				.totalPages(1).totalChunks(2).status(DocumentStatus.INDEXED).build();
	}

	// ====================================================
	// SINGLE DOCUMENT UPLOAD
	// ====================================================

	@Test
	void uploadAndProcess_shouldUploadAndProcessSuccessfully() {

		MockMultipartFile file = createFile("test.txt", "Document content");

		when(parserService.parse(file)).thenReturn(List.of(new Document("Document content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenAnswer(invocation -> {
			DocumentMetadata metadata = invocation.getArgument(0);
			metadata.setStatus(DocumentStatus.INDEXED);
			return 3;
		});

		DocumentResponseDto response = documentMetadataService.uploadAndProcess(file, user);

		assertNotNull(response);
		assertNotNull(response.getId());
		assertEquals("test.txt", response.getFileName());
		assertEquals(file.getSize(), response.getFileSize());
		assertEquals(3, response.getChunksCreated());
		assertEquals(DocumentStatus.INDEXED, response.getStatus());
		assertEquals("Document successfully processed and indexed", response.getMessage());

		verify(persistenceService).create(eq("test.txt"), eq("text/plain"), eq(file.getSize()), eq(user.getId()));
		verify(parserService).parse(file);
		verify(ingestionService).ingest(any(DocumentMetadata.class), anyList());
	}

	@Test
	void uploadAndProcess_shouldRejectNullUser() {

		MockMultipartFile file = createFile("test.txt", "content");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.uploadAndProcess(file, null));

		assertEquals("Authenticated user is required", exception.getMessage());

		verifyNoInteractions(documentMetadataRepository);
		verifyNoInteractions(parserService);
		verifyNoInteractions(ingestionService);
	}

	@Test
	void uploadAndProcess_shouldRejectUserWithoutId() {

		User invalidUser = User.builder().username("testuser").email("test@example.com").build();

		MockMultipartFile file = createFile("test.txt", "content");

		assertThrows(IllegalArgumentException.class, () -> documentMetadataService.uploadAndProcess(file, invalidUser));

		verifyNoInteractions(documentMetadataRepository);
		verifyNoInteractions(parserService);
	}

	@Test
	void uploadAndProcess_shouldRejectNullFile() {

		assertThrows(IllegalArgumentException.class, () -> documentMetadataService.uploadAndProcess(null, user));

		verifyNoInteractions(persistenceService);
		verifyNoInteractions(parserService);
	}

	@Test
	void uploadAndProcess_shouldRejectEmptyFile() {

		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

		assertThrows(IllegalArgumentException.class, () -> documentMetadataService.uploadAndProcess(emptyFile, user));

		verifyNoInteractions(persistenceService);
		verifyNoInteractions(parserService);
	}

	@Test
	void uploadAndProcess_shouldUseDefaultFilenameWhenFilenameIsNull() {

		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getOriginalFilename()).thenReturn(null);
		when(file.getContentType()).thenReturn("text/plain");
		when(file.getSize()).thenReturn(7L);

		when(parserService.parse(file)).thenReturn(List.of(new Document("content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenReturn(2);

		documentMetadataService.uploadAndProcess(file, user);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

		verify(persistenceService).create(captor.capture(), anyString(), any(Long.class), eq(user.getId()));

		assertEquals("document", captor.getValue());
	}

	@Test
	void uploadAndProcess_shouldUseDefaultContentTypeWhenContentTypeIsNull() {

		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getOriginalFilename()).thenReturn("test.txt");
		when(file.getContentType()).thenReturn(null);
		when(file.getSize()).thenReturn(7L);

		when(parserService.parse(file)).thenReturn(List.of(new Document("content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenReturn(2);

		documentMetadataService.uploadAndProcess(file, user);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

		verify(persistenceService).create(anyString(), captor.capture(), any(Long.class), eq(user.getId()));

		assertEquals("application/octet-stream", captor.getValue());
	}

	@Test
	void uploadAndProcess_shouldSaveMetadataWithUploadingStatus() {

		MockMultipartFile file = createFile("test.txt", "content");

		when(parserService.parse(file)).thenReturn(List.of(new Document("content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenAnswer(invocation -> {
			DocumentMetadata metadata = invocation.getArgument(0);
			metadata.setStatus(DocumentStatus.INDEXED);
			return 2;
		});

		documentMetadataService.uploadAndProcess(file, user);

		verify(persistenceService).create(eq("test.txt"), eq("text/plain"), eq(file.getSize()), eq(user.getId()));
	}

	@Test
	void uploadAndProcess_shouldPropagateDocumentProcessingException() {

		MockMultipartFile file = createFile("test.txt", "content");

		DocumentProcessingException expected = new DocumentProcessingException("Parsing failed");

		when(parserService.parse(file)).thenThrow(expected);

		DocumentProcessingException actual = assertThrows(DocumentProcessingException.class,
				() -> documentMetadataService.uploadAndProcess(file, user));

		assertSame(expected, actual);

		verify(ingestionService, never()).ingest(any(), anyList());
	}

	@Test
	void uploadAndProcess_shouldWrapUnexpectedException() {

		MockMultipartFile file = createFile("test.txt", "content");

		when(parserService.parse(file)).thenThrow(new RuntimeException("Unexpected parser error"));

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentMetadataService.uploadAndProcess(file, user));

		assertEquals("Failed to process uploaded document", exception.getMessage());

		assertInstanceOf(RuntimeException.class, exception.getCause());

		verify(ingestionService, never()).ingest(any(), anyList());
	}

	@Test
	void uploadAndProcess_shouldRejectZeroGeneratedChunks() {

		MockMultipartFile file = createFile("test.txt", "content");

		when(parserService.parse(file)).thenReturn(List.of(new Document("content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenReturn(0);

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentMetadataService.uploadAndProcess(file, user));

		assertEquals("No text chunks generated", exception.getMessage());
	}

	@Test
	void uploadAndProcess_shouldRejectNegativeGeneratedChunks() {

		MockMultipartFile file = createFile("test.txt", "content");

		when(parserService.parse(file)).thenReturn(List.of(new Document("content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenReturn(-1);

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentMetadataService.uploadAndProcess(file, user));

		assertEquals("No text chunks generated", exception.getMessage());
	}

	// ====================================================
	// MULTIPLE DOCUMENT UPLOAD
	// ====================================================

	@Test
	void uploadMultipleDocuments_shouldProcessAllFiles() {

		MockMultipartFile file1 = createFile("first.txt", "first content");

		MockMultipartFile file2 = createFile("second.txt", "second content");

		when(parserService.parse(any())).thenReturn(List.of(new Document("content")));

		when(ingestionService.ingest(any(DocumentMetadata.class), anyList())).thenAnswer(invocation -> {
			DocumentMetadata metadata = invocation.getArgument(0);
			metadata.setStatus(DocumentStatus.INDEXED);
			return 2;
		});

		List<DocumentResponseDto> responses = documentMetadataService.uploadMultipleDocuments(List.of(file1, file2),
				user);

		assertEquals(2, responses.size());
		assertEquals("first.txt", responses.get(0).getFileName());
		assertEquals("second.txt", responses.get(1).getFileName());

		verify(persistenceService, times(2)).create(anyString(), anyString(), any(Long.class), eq(user.getId()));
		verify(parserService, times(2)).parse(any());
		verify(ingestionService, times(2)).ingest(any(), anyList());
	}

	@Test
	void uploadMultipleDocuments_shouldRejectNullFileList() {

		assertThrows(IllegalArgumentException.class, () -> documentMetadataService.uploadMultipleDocuments(null, user));

		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void uploadMultipleDocuments_shouldRejectEmptyFileList() {

		assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.uploadMultipleDocuments(List.of(), user));

		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void uploadMultipleDocuments_shouldRejectNullUser() {

		MockMultipartFile file = createFile("test.txt", "content");

		assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.uploadMultipleDocuments(List.of(file), null));

		verifyNoInteractions(documentMetadataRepository);
	}

	// ====================================================
	// GET ALL DOCUMENTS
	// ====================================================

	@Test
	void getAllDocuments_shouldReturnUserDocuments() {

		DocumentMetadata metadata1 = createMetadata(UUID.randomUUID());
		DocumentMetadata metadata2 = createMetadata(UUID.randomUUID());

		DocumentMetadataDto dto1 = createMetadataDto(metadata1.getId());
		DocumentMetadataDto dto2 = createMetadataDto(metadata2.getId());

		when(documentMetadataRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(metadata1, metadata2));

		when(modelMapper.map(metadata1, DocumentMetadataDto.class)).thenReturn(dto1);

		when(modelMapper.map(metadata2, DocumentMetadataDto.class)).thenReturn(dto2);

		List<DocumentMetadataDto> result = documentMetadataService.getAllDocuments(user);

		assertEquals(2, result.size());
		assertEquals(metadata1.getId(), result.get(0).getId());
		assertEquals(metadata2.getId(), result.get(1).getId());

		verify(documentMetadataRepository).findByUserOrderByCreatedAtDesc(user);
	}

	@Test
	void getAllDocuments_shouldReturnEmptyListWhenNoDocumentsExist() {

		when(documentMetadataRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());

		List<DocumentMetadataDto> result = documentMetadataService.getAllDocuments(user);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getAllDocuments_shouldRejectNullUser() {

		assertThrows(IllegalArgumentException.class, () -> documentMetadataService.getAllDocuments(null));

		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void getAllDocuments_shouldRejectUserWithoutId() {

		User invalidUser = User.builder().username("testuser").build();

		assertThrows(IllegalArgumentException.class, () -> documentMetadataService.getAllDocuments(invalidUser));

		verifyNoInteractions(documentMetadataRepository);
	}

	// ====================================================
	// GET DOCUMENT BY ID
	// ====================================================

	@Test
	void getDocumentById_shouldReturnDocumentWhenOwnedByUser() {

		UUID documentId = UUID.randomUUID();

		DocumentMetadata metadata = createMetadata(documentId);
		DocumentMetadataDto expectedDto = createMetadataDto(documentId);

		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.of(metadata));

		when(modelMapper.map(metadata, DocumentMetadataDto.class)).thenReturn(expectedDto);

		DocumentMetadataDto result = documentMetadataService.getDocumentById(documentId, user);

		assertNotNull(result);
		assertEquals(documentId, result.getId());

		verify(documentMetadataRepository).findByIdAndUser(documentId, user);
	}

	@Test
	void getDocumentById_shouldThrowWhenDocumentDoesNotExist() {

		UUID documentId = UUID.randomUUID();

		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> documentMetadataService.getDocumentById(documentId, user));

		verify(modelMapper, never()).map(any(), any());
	}

	@Test
	void getDocumentById_shouldRejectNullUser() {

		assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.getDocumentById(UUID.randomUUID(), null));

		verifyNoInteractions(documentMetadataRepository);
	}

	@Test
	void getDocumentById_shouldRejectUserWithoutId() {

		User invalidUser = User.builder().username("testuser").build();

		assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.getDocumentById(UUID.randomUUID(), invalidUser));

		verifyNoInteractions(documentMetadataRepository);
	}

	// ====================================================
	// DELETE DOCUMENT
	// ====================================================

	@Test
	void deleteDocument_shouldDeleteVectorsAndMetadata() {

		UUID documentId = UUID.randomUUID();

		DocumentMetadata metadata = createMetadata(documentId);

		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.of(metadata));

		when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(2);

		documentMetadataService.deleteDocument(documentId, user);

		verify(jdbcTemplate).update(contains("DELETE FROM vector_store"), eq(documentId.toString()));

		verify(documentMetadataRepository).delete(metadata);
	}

	@Test
	void deleteDocument_shouldDeleteMetadataWhenNoVectorsExist() {

		UUID documentId = UUID.randomUUID();

		DocumentMetadata metadata = createMetadata(documentId);

		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.of(metadata));

		when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

		assertDoesNotThrow(() -> documentMetadataService.deleteDocument(documentId, user));

		verify(documentMetadataRepository).delete(metadata);
	}

	@Test
	void deleteDocument_shouldNotDeleteWhenDocumentDoesNotExist() {

		UUID documentId = UUID.randomUUID();

		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> documentMetadataService.deleteDocument(documentId, user));

		verifyNoInteractions(jdbcTemplate);
		verify(documentMetadataRepository, never()).delete(any());
	}

	@Test
	void deleteDocument_shouldRejectNullUser() {

		assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.deleteDocument(UUID.randomUUID(), null));

		verifyNoInteractions(documentMetadataRepository);
		verifyNoInteractions(jdbcTemplate);
	}

	@Test
	void deleteDocument_shouldRejectUserWithoutId() {

		User invalidUser = User.builder().username("testuser").build();

		assertThrows(IllegalArgumentException.class,
				() -> documentMetadataService.deleteDocument(UUID.randomUUID(), invalidUser));

		verifyNoInteractions(documentMetadataRepository);
		verifyNoInteractions(jdbcTemplate);
	}

	@Test
	void deleteDocument_shouldNotDeleteMetadataWhenVectorDeletionFails() {

		UUID documentId = UUID.randomUUID();

		DocumentMetadata metadata = createMetadata(documentId);

		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.of(metadata));

		when(jdbcTemplate.update(anyString(), any(Object[].class))).thenThrow(new RuntimeException("Database error"));

		assertThrows(RuntimeException.class, () -> documentMetadataService.deleteDocument(documentId, user));

		verify(documentMetadataRepository, never()).delete(any());
	}
}
