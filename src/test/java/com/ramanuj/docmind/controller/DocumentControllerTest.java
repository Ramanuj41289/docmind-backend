
package com.ramanuj.docmind.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ramanuj.docmind.dto.DocumentMetadataDto;
import com.ramanuj.docmind.dto.DocumentResponseDto;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.GlobalExceptionHandler;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.service.DocumentMetadataService;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

	@Mock
	private DocumentMetadataService documentService;

	@InjectMocks
	private DocumentController documentController;

	private MockMvc mockMvc;

	private User user;

	private Authentication authentication;

	@BeforeEach
	void setUp() {

		mockMvc = MockMvcBuilders.standaloneSetup(documentController).setControllerAdvice(new GlobalExceptionHandler())
				.build();

		user = User.builder().id(1L).username("testuser").email("testuser@example.com").password("password")
				.role(Role.USER).build();

		authentication = new UsernamePasswordAuthenticationToken(user, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void upload_shouldReturnCreatedResponse() throws Exception {

		UUID documentId = UUID.randomUUID();

		MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
				"Sample PDF content".getBytes());

		DocumentResponseDto response = DocumentResponseDto.builder().id(documentId).fileName("test.pdf").fileSize(19L)
				.chunksCreated(3).status(DocumentStatus.INDEXED).message("Document successfully processed and indexed")
				.build();

		when(documentService.uploadAndProcess(any(), eq(user))).thenReturn(response);

		mockMvc.perform(multipart("/api/v1/documents/upload").file(file).principal(authentication))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("File indexed successfully"))
				.andExpect(jsonPath("$.data.id").value(documentId.toString()))
				.andExpect(jsonPath("$.data.fileName").value("test.pdf"))
				.andExpect(jsonPath("$.data.chunksCreated").value(3))
				.andExpect(jsonPath("$.data.status").value("INDEXED")).andExpect(jsonPath("$.timestamp").exists());

		verify(documentService).uploadAndProcess(any(), eq(user));
	}

	@Test
	void uploadMultiple_shouldReturnCreatedResponse() throws Exception {

		MockMultipartFile firstFile = new MockMultipartFile("files", "first.pdf", MediaType.APPLICATION_PDF_VALUE,
				"First PDF".getBytes());

		MockMultipartFile secondFile = new MockMultipartFile("files", "second.pdf", MediaType.APPLICATION_PDF_VALUE,
				"Second PDF".getBytes());

		DocumentResponseDto firstResponse = DocumentResponseDto.builder().id(UUID.randomUUID()).fileName("first.pdf")
				.fileSize(9L).chunksCreated(2).status(DocumentStatus.INDEXED).build();

		DocumentResponseDto secondResponse = DocumentResponseDto.builder().id(UUID.randomUUID()).fileName("second.pdf")
				.fileSize(10L).chunksCreated(3).status(DocumentStatus.INDEXED).build();

		when(documentService.uploadMultipleDocuments(any(), eq(user)))
				.thenReturn(List.of(firstResponse, secondResponse));

		mockMvc.perform(multipart("/api/v1/documents/upload-multiple").file(firstFile).file(secondFile)
				.principal(authentication)).andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("All documents uploaded and indexed successfully."))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].fileName").value("first.pdf"))
				.andExpect(jsonPath("$.data[1].fileName").value("second.pdf"));

		verify(documentService).uploadMultipleDocuments(any(), eq(user));
	}

	@Test
	void getAll_shouldReturnDocumentList() throws Exception {

		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();

		DocumentMetadataDto firstDocument = DocumentMetadataDto.builder().id(firstId).filename("first.pdf")
				.contentType(MediaType.APPLICATION_PDF_VALUE).fileSize(100L).totalPages(5).totalChunks(10)
				.status(DocumentStatus.INDEXED).createdAt(LocalDateTime.now()).build();

		DocumentMetadataDto secondDocument = DocumentMetadataDto.builder().id(secondId).filename("second.pdf")
				.contentType(MediaType.APPLICATION_PDF_VALUE).fileSize(200L).totalPages(8).totalChunks(15)
				.status(DocumentStatus.INDEXED).createdAt(LocalDateTime.now()).build();

		when(documentService.getAllDocuments(user)).thenReturn(List.of(firstDocument, secondDocument));

		mockMvc.perform(get("/api/v1/documents").principal(authentication)).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Documents retrieved successfully"))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].id").value(firstId.toString()))
				.andExpect(jsonPath("$.data[0].filename").value("first.pdf"))
				.andExpect(jsonPath("$.data[1].filename").value("second.pdf"));

		verify(documentService).getAllDocuments(user);
	}

	@Test
	void getAll_shouldReturnEmptyListWhenNoDocumentsExist() throws Exception {

		when(documentService.getAllDocuments(user)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/documents").principal(authentication)).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.length()").value(0));

		verify(documentService).getAllDocuments(user);
	}

	@Test
	void getById_shouldReturnDocument() throws Exception {

		UUID documentId = UUID.randomUUID();

		DocumentMetadataDto document = DocumentMetadataDto.builder().id(documentId).filename("annual-report.pdf")
				.contentType(MediaType.APPLICATION_PDF_VALUE).fileSize(500L).totalPages(20).totalChunks(40)
				.status(DocumentStatus.INDEXED).createdAt(LocalDateTime.now()).build();

		when(documentService.getDocumentById(documentId, user)).thenReturn(document);

		mockMvc.perform(get("/api/v1/documents/{id}", documentId).principal(authentication)).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Document retrieved successfully"))
				.andExpect(jsonPath("$.data.id").value(documentId.toString()))
				.andExpect(jsonPath("$.data.filename").value("annual-report.pdf"))
				.andExpect(jsonPath("$.data.status").value("INDEXED"));

		verify(documentService).getDocumentById(documentId, user);
	}

	@Test
	void getById_shouldReturnNotFoundWhenDocumentDoesNotExist() throws Exception {

		UUID documentId = UUID.randomUUID();

		when(documentService.getDocumentById(documentId, user))
				.thenThrow(new ResourceNotFoundException("Document not found"));

		mockMvc.perform(get("/api/v1/documents/{id}", documentId).principal(authentication))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Document not found"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(documentService).getDocumentById(documentId, user);
	}

	@Test
	void delete_shouldReturnSuccessResponse() throws Exception {

		UUID documentId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/documents/{id}", documentId).principal(authentication))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Document deleted successfully"))
				.andExpect(jsonPath("$.timestamp").exists());

		verify(documentService).deleteDocument(documentId, user);
	}

	@Test
	void upload_shouldReturnBadRequestWhenFileIsEmpty() throws Exception {

		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE,
				new byte[0]);

		when(documentService.uploadAndProcess(any(), eq(user)))
				.thenThrow(new IllegalArgumentException("Uploaded file cannot be empty"));

		mockMvc.perform(multipart("/api/v1/documents/upload").file(emptyFile).principal(authentication))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Uploaded file cannot be empty"));

		verify(documentService).uploadAndProcess(any(), eq(user));
	}

	@Test
	void getAll_shouldReturnForbiddenWhenAuthenticationIsMissing() throws Exception {

		mockMvc.perform(get("/api/v1/documents")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
	}
}
