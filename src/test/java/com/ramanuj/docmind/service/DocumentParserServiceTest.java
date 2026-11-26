
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ramanuj.docmind.exception.DocumentProcessingException;

class DocumentParserServiceTest {

	private DocumentParserService documentParserService;

	@BeforeEach
	void setUp() {
		documentParserService = new DocumentParserService();
	}

	@Test
	void parseShouldRejectNullFile() {
		assertThrows(DocumentProcessingException.class, () -> documentParserService.parse(null));
	}

	@Test
	void parseShouldRejectEmptyFile() {
		MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

		assertThrows(DocumentProcessingException.class, () -> documentParserService.parse(file));
	}

	@Test
	void parseShouldRejectUnsupportedFileType() {
		MockMultipartFile file = new MockMultipartFile("file", "document.exe", "application/octet-stream",
				"unsupported content".getBytes(StandardCharsets.UTF_8));

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentParserService.parse(file));

		assertEquals("Unsupported file type. Supported formats: PDF, DOC, DOCX, TXT, MD and CSV",
				exception.getMessage());
	}

	@Test
	void parseShouldSuccessfullyParseTextFile() {
		String content = "DocMind document parsing test.";

		MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain",
				content.getBytes(StandardCharsets.UTF_8));

		List<Document> documents = documentParserService.parse(file);

		assertFalse(documents.isEmpty());

		String extractedText = documents.stream().map(document -> document.getText()).reduce("",
				(first, second) -> first + second);

		assertEquals(content, extractedText.trim());
	}

	@Test
	void parseShouldSuccessfullyParseMarkdownFile() {
		String content = """
				# DocMind

				This is a Markdown document.

				## Features

				- Document upload
				- Semantic search
				- RAG
				""";

		MockMultipartFile file = new MockMultipartFile("file", "sample.md", "text/markdown",
				content.getBytes(StandardCharsets.UTF_8));

		List<Document> documents = documentParserService.parse(file);

		assertFalse(documents.isEmpty());

		String extractedText = documents.stream().map(document -> document.getText()).reduce("",
				(first, second) -> first + second);

		assertFalse(extractedText.isBlank());
		assertTrue(extractedText.contains("DocMind"));
		assertTrue(extractedText.contains("Semantic search"));
	}

	@Test
	void parseShouldSuccessfullyParseCsvFile() {
		String content = """
				id,name,department
				1,Ramanuj,Engineering
				2,John,Finance
				""";

		MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv",
				content.getBytes(StandardCharsets.UTF_8));

		List<Document> documents = documentParserService.parse(file);

		assertFalse(documents.isEmpty());

		String extractedText = documents.stream().map(document -> document.getText()).reduce("",
				(first, second) -> first + second);

		assertTrue(extractedText.contains("Ramanuj"));
		assertTrue(extractedText.contains("Engineering"));
	}

	@Test
	void parseShouldUseFilenameWhenContentTypeIsMissing() {
		String content = "Testing filename based parsing.";

		MockMultipartFile file = new MockMultipartFile("file", "sample.txt", null,
				content.getBytes(StandardCharsets.UTF_8));

		List<Document> documents = documentParserService.parse(file);

		assertFalse(documents.isEmpty());

		String extractedText = documents.stream().map(document -> document.getText()).reduce("",
				(first, second) -> first + second);

		assertTrue(extractedText.contains(content));
	}

	@Test
	void parseShouldUseContentTypeWhenFilenameHasNoExtension() {
		String content = "Testing content type based parsing.";

		MockMultipartFile file = new MockMultipartFile("file", "document", "text/plain",
				content.getBytes(StandardCharsets.UTF_8));

		List<Document> documents = documentParserService.parse(file);

		assertFalse(documents.isEmpty());

		String extractedText = documents.stream().map(document -> document.getText()).reduce("",
				(first, second) -> first + second);

		assertTrue(extractedText.contains(content));
	}

	@Test
	void parseShouldHandleNullOriginalFilename() {
		MockMultipartFile file = new MockMultipartFile("file", null, null,
				"sample content".getBytes(StandardCharsets.UTF_8));

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentParserService.parse(file));

		assertTrue(exception.getMessage().contains("Unsupported file type"));
	}

	@Test
	void parseShouldHandleUppercaseFileExtension() {
		String content = "Testing uppercase extension.";

		MockMultipartFile file = new MockMultipartFile("file", "SAMPLE.TXT", "application/octet-stream",
				content.getBytes(StandardCharsets.UTF_8));

		List<Document> documents = documentParserService.parse(file);

		assertFalse(documents.isEmpty());

		String extractedText = documents.stream().map(document -> document.getText()).reduce("",
				(first, second) -> first + second);

		assertTrue(extractedText.contains(content));
	}

	@Test
	void parseShouldHandlePdfContentTypeWithoutPdfExtension() {
		MockMultipartFile file = new MockMultipartFile("file", "document", "application/pdf",
				"invalid pdf content".getBytes(StandardCharsets.UTF_8));

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentParserService.parse(file));

		assertEquals("Could not parse uploaded file", exception.getMessage());
	}

	@Test
	void parseShouldWrapIOException() throws IOException {
		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getOriginalFilename()).thenReturn("sample.txt");
		when(file.getContentType()).thenReturn("text/plain");
		when(file.getBytes()).thenThrow(new IOException("Read failed"));

		DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
				() -> documentParserService.parse(file));

		assertEquals("Could not read uploaded file", exception.getMessage());
		assertTrue(exception.getCause() instanceof IOException);
	}
}
