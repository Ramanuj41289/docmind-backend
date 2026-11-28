
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.dto.ChatRequestDto;
import com.ramanuj.docmind.dto.SearchRequestDto;
import com.ramanuj.docmind.dto.SearchResultDto;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.exception.VectorSearchException;
import com.ramanuj.docmind.repository.ConversationRepository;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

	@Mock
	private VectorStore vectorStore;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ChatClient chatClient;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private DocumentMetadataRepository documentMetadataRepository;

	@Mock
	private SessionHistoryMigrationService sessionHistoryMigrationService;

	@Mock
	private ConversationHistoryWriter conversationHistoryWriter;

	private AppProperties appProperties;
	private RagService ragService;
	private User user;

	@BeforeEach
	void setUp() {
		appProperties = new AppProperties();
		ragService = new RagService(vectorStore, appProperties, chatClient, conversationRepository,
				documentMetadataRepository, sessionHistoryMigrationService, conversationHistoryWriter);
		user = User.builder().id(1L).username("testuser").email("testuser@example.com").password("password").build();
	}

	// --------------------------------------------------
	// Similarity search tests
	// --------------------------------------------------

	@Test
	void searchSimilarChunks_shouldReturnMatchingDocuments() {
		UUID documentId = UUID.randomUUID();
		Document document = new Document("Spring Boot is a Java framework.", Map.of("documentId", documentId.toString(),
				"fileName", "spring-guide.pdf", "chunkIndex", 2, "pageNumber", 5, "distance", 0.2));
		SearchRequestDto request = SearchRequestDto.builder().query("What is Spring Boot?").topK(5).build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertNotNull(result);
		assertEquals("What is Spring Boot?", result.getQuery());
		assertEquals(1, result.getTotalMatches());
		assertEquals(1, result.getMatches().size());
		var citation = result.getMatches().get(0);
		assertEquals(documentId, citation.getDocumentId());
		assertEquals("spring-guide.pdf", citation.getFileName());
		assertEquals(2, citation.getChunkIndex());
		assertEquals(5, citation.getPageNumber());
		assertEquals("Spring Boot is a Java framework.", citation.getSnippet());
		assertEquals(0.8, citation.getSimilarityScore(), 0.0001);
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void searchSimilarChunks_shouldReturnEmptyResultWhenNoDocumentsFound() {
		SearchRequestDto request = SearchRequestDto.builder().query("Unknown topic").build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertNotNull(result);
		assertEquals("Unknown topic", result.getQuery());
		assertEquals(0, result.getTotalMatches());
		assertTrue(result.getMatches().isEmpty());
	}

	@Test
	void searchSimilarChunks_shouldHandleInvalidDocumentIdInMetadata() {
		Document document = new Document("Document content",
				Map.of("documentId", "invalid-uuid", "fileName", "test.pdf", "page_number", 7));
		SearchRequestDto request = SearchRequestDto.builder().query("Find document").build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertEquals(1, result.getTotalMatches());
		var citation = result.getMatches().get(0);
		assertNull(citation.getDocumentId());
		assertEquals(7, citation.getPageNumber());
		assertEquals("test.pdf", citation.getFileName());
	}

	@Test
	void searchSimilarChunks_shouldUseDefaultValuesForMissingMetadata() {
		Document document = new Document("Some document content", Map.of());
		SearchRequestDto request = SearchRequestDto.builder().query("Find content").build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		var citation = result.getMatches().get(0);
		assertEquals("Unknown", citation.getFileName());
		assertNull(citation.getDocumentId());
		assertNull(citation.getChunkIndex());
		assertNull(citation.getPageNumber());
		assertNull(citation.getSimilarityScore());
		assertEquals("Some document content", citation.getSnippet());
	}

	@Test
	void searchSimilarChunks_shouldReturnNullSimilarityWhenDistanceIsMissing() {
		Document document = new Document("Document content", Map.of("fileName", "test.pdf", "chunkIndex", 1));
		SearchRequestDto request = SearchRequestDto.builder().query("Search content").build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertNull(result.getMatches().get(0).getSimilarityScore());
	}

	@Test
	void searchSimilarChunks_shouldValidateDocumentOwnership() {
		UUID documentId = UUID.randomUUID();
		SearchRequestDto request = SearchRequestDto.builder().query("Search document").documentId(documentId).build();
		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> ragService.searchSimilarChunks(request, user));
		verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void searchSimilarChunks_shouldWrapVectorStoreException() {
		SearchRequestDto request = SearchRequestDto.builder().query("Search content").build();
		RuntimeException originalException = new RuntimeException("Vector database unavailable");
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(originalException);
		VectorSearchException exception = assertThrows(VectorSearchException.class,
				() -> ragService.searchSimilarChunks(request, user));
		assertEquals("Vector search failed", exception.getMessage());
		assertSame(originalException, exception.getCause());
	}

	@Test
	void searchSimilarChunks_shouldRejectNullUser() {
		SearchRequestDto request = SearchRequestDto.builder().query("Search content").build();
		assertThrows(IllegalArgumentException.class, () -> ragService.searchSimilarChunks(request, null));
		verifyNoInteractions(vectorStore);
	}

	@Test
	void searchSimilarChunks_shouldRejectUserWithoutId() {
		User invalidUser = User.builder().username("testuser").build();
		SearchRequestDto request = SearchRequestDto.builder().query("Search content").build();

		assertThrows(IllegalArgumentException.class, () -> ragService.searchSimilarChunks(request, invalidUser));
		verifyNoInteractions(vectorStore);
	}

	// --------------------------------------------------
	// User validation tests
	// --------------------------------------------------

	@Test
	void askQuestion_shouldRejectNullUser() {
		assertThrows(IllegalArgumentException.class, () -> ragService.askQuestion(null, null));
		verifyNoInteractions(vectorStore, chatClient, conversationRepository, sessionHistoryMigrationService,
				conversationHistoryWriter);
	}

	@Test
	void streamQuestionAnswer_shouldRejectNullUser() {
		assertThrows(IllegalArgumentException.class, () -> ragService.streamQuestionAnswer(null, null));
		verifyNoInteractions(vectorStore, chatClient, conversationRepository, sessionHistoryMigrationService,
				conversationHistoryWriter);
	}

	@Test
	void askQuestion_shouldRejectUserWithoutId() {
		User invalidUser = User.builder().username("testuser").build();
		assertThrows(IllegalArgumentException.class, () -> ragService.askQuestion(null, invalidUser));
		verifyNoInteractions(vectorStore, chatClient, conversationRepository, sessionHistoryMigrationService,
				conversationHistoryWriter);
	}

	@Test
	void streamQuestionAnswer_shouldRejectUserWithoutId() {
		User invalidUser = User.builder().username("testuser").build();
		assertThrows(IllegalArgumentException.class, () -> ragService.streamQuestionAnswer(null, invalidUser));
		verifyNoInteractions(vectorStore, chatClient, conversationRepository, sessionHistoryMigrationService,
				conversationHistoryWriter);
	}

	// --------------------------------------------------
	// Non-streaming chat tests
	// --------------------------------------------------

	@Test
	void askQuestion_shouldReturnAnswerAndCitations() {
		UUID documentId = UUID.randomUUID();
		Document firstDocument = new Document("Revenue increased by 15%.", Map.of("documentId", documentId.toString(),
				"fileName", "annual-report.pdf", "chunkIndex", 1, "pageNumber", 4, "distance", 0.15));
		Document secondDocument = new Document("Operating expenses decreased by 5%.",
				Map.of("documentId", documentId.toString(), "fileName", "annual-report.pdf", "chunkIndex", 2,
						"pageNumber", 5, "distance", 0.20));
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(firstDocument, secondDocument));
		stubNewConversation();
		stubChatResponse("Revenue increased by 15%.");
		ChatRequestDto request = ChatRequestDto.builder().question("What was the revenue growth?").build();
		var response = ragService.askQuestion(request, user);
		assertNotNull(response);
		assertEquals("Revenue increased by 15%.", response.getAnswer());
		assertNotNull(response.getConversationId());
		assertFalse(response.getConversationId().isBlank());
		assertEquals(2, response.getCitations().size());
		assertEquals("annual-report.pdf", response.getCitations().get(0).getFileName());
		assertEquals(4, response.getCitations().get(0).getPageNumber());
		assertEquals(5, response.getCitations().get(1).getPageNumber());
		assertTrue(response.getResponseTimeMs() >= 0);
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
		verify(conversationRepository).save(any(Conversation.class));
		verify(sessionHistoryMigrationService).ensureSessionReady(response.getConversationId(), user);
		verify(conversationHistoryWriter).saveUserMessage(response.getConversationId(), "What was the revenue growth?");
		verify(conversationHistoryWriter).saveAssistantMessage(response.getConversationId(),
				"Revenue increased by 15%.");
	}

	@Test
	void askQuestion_shouldHandleEmptyRetrievedContext() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatResponse("No relevant information was found.");
		ChatRequestDto request = ChatRequestDto.builder().question("Explain the company").build();
		var response = ragService.askQuestion(request, user);
		assertNotNull(response);
		assertEquals("No relevant information was found.", response.getAnswer());
		assertTrue(response.getCitations().isEmpty());
		assertNotNull(response.getConversationId());
		verify(conversationHistoryWriter).saveUserMessage(response.getConversationId(), "Explain the company");
		verify(conversationHistoryWriter).saveAssistantMessage(response.getConversationId(),
				"No relevant information was found.");
	}

	@Test
	void askQuestion_shouldNotSaveBlankAssistantResponse() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatResponse(" ");
		ChatRequestDto request = ChatRequestDto.builder().question("Explain the company").build();
		var response = ragService.askQuestion(request, user);
		assertNotNull(response);
		verify(conversationHistoryWriter).saveUserMessage(anyString(), eq("Explain the company"));
		verify(conversationHistoryWriter, never()).saveAssistantMessage(anyString(), anyString());
	}

	@Test
	void askQuestion_shouldIncludeEvalMetadataByDefault() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatResponse("No relevant information was found.");
		ChatRequestDto request = ChatRequestDto.builder().question("Explain the company").build();
		var response = ragService.askQuestion(request, user);
		assertNotNull(response.getEvalMetadata());
		assertEquals(0, response.getEvalMetadata().get("retrievedChunkCount"));
		assertEquals(0, response.getEvalMetadata().get("citationCount"));
		assertEquals(false, response.getEvalMetadata().get("hasGrounding"));
	}

	@Test
	void askQuestion_shouldSkipEvalMetadataWhenDisabled() {
		appProperties.getEval().setIncludeMetadata(false);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatResponse("Answer");
		ChatRequestDto request = ChatRequestDto.builder().question("Explain the company").build();
		var response = ragService.askQuestion(request, user);
		assertNull(response.getEvalMetadata());
	}

	// --------------------------------------------------
	// Streaming chat tests
	// --------------------------------------------------

	@Test
	void streamQuestionAnswer_shouldEmitTokensAndComplete() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatStream(Flux.just("Revenue ", "increased ", "by 15%."));
		ChatRequestDto request = ChatRequestDto.builder().question("Summarize the document").build();
		List<String> tokens = ragService.streamQuestionAnswer(request, user).collectList().block();
		assertNotNull(tokens);
		assertEquals(List.of("Revenue ", "increased ", "by 15%."), tokens);
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
		verify(conversationRepository).save(any(Conversation.class));
		ArgumentCaptor<String> conversationIdCaptor = ArgumentCaptor.forClass(String.class);
		verify(sessionHistoryMigrationService).ensureSessionReady(conversationIdCaptor.capture(), eq(user));
		String conversationId = conversationIdCaptor.getValue();
		verify(conversationHistoryWriter).saveUserMessage(conversationId, "Summarize the document");
		verify(conversationHistoryWriter).saveAssistantMessage(conversationId, "Revenue increased by 15%.");
	}

	@Test
	void streamQuestionAnswer_shouldPropagateStreamError() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatStream(Flux.error(new IllegalStateException("LLM streaming failed")));
		ChatRequestDto request = ChatRequestDto.builder().question("Summarize the document").build();
		assertThrows(IllegalStateException.class,
				() -> ragService.streamQuestionAnswer(request, user).collectList().block());
		verify(conversationHistoryWriter).saveUserMessage(anyString(), eq("Summarize the document"));
		verify(conversationHistoryWriter, never()).saveAssistantMessage(anyString(), anyString());
	}

	@Test
	void streamQuestionAnswer_shouldHandleCancellation() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatStream(Flux.concat(Flux.just("First token"), Flux.never()));
		ChatRequestDto request = ChatRequestDto.builder().question("Summarize the document").build();
		String firstToken = ragService.streamQuestionAnswer(request, user).take(1).blockFirst();
		assertEquals("First token", firstToken);
		verify(conversationHistoryWriter).saveUserMessage(anyString(), eq("Summarize the document"));
		verify(conversationHistoryWriter, never()).saveAssistantMessage(anyString(), anyString());
	}

	@Test
	void streamQuestionAnswer_shouldSaveHistoryOnlyOncePerSubscription() {
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatStream(Flux.just("Answer"));
		ChatRequestDto request = ChatRequestDto.builder().question("Test subscription").build();
		Flux<String> response = ragService.streamQuestionAnswer(request, user);
		response.collectList().block();
		response.collectList().block();
		verify(conversationHistoryWriter, times(2)).saveUserMessage(anyString(), eq("Test subscription"));
		verify(conversationHistoryWriter, times(2)).saveAssistantMessage(anyString(), eq("Answer"));
	}

	// --------------------------------------------------
	// Conversation ownership and title tests
	// --------------------------------------------------

	@Test
	void askQuestion_shouldReuseExistingConversationWithoutSavingAnother() {
		String conversationId = "existing-conversation";
		Conversation existingConversation = Conversation.builder().id(conversationId).title("Existing conversation")
				.user(user).build();
		when(conversationRepository.findByIdAndUser(conversationId, user))
				.thenReturn(Optional.of(existingConversation));
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubChatResponse("Answer from existing conversation.");
		ChatRequestDto request = ChatRequestDto.builder().question("Continue this conversation")
				.conversationId(conversationId).build();
		var response = ragService.askQuestion(request, user);
		assertEquals(conversationId, response.getConversationId());
		assertEquals("Answer from existing conversation.", response.getAnswer());
		verify(conversationRepository, never()).save(any(Conversation.class));
		verify(sessionHistoryMigrationService).ensureSessionReady(conversationId, user);
	}

	@Test
	void askQuestion_shouldRejectConversationOwnedByAnotherUser() {
		String conversationId = "another-users-conversation";
		when(conversationRepository.findByIdAndUser(conversationId, user)).thenReturn(Optional.empty());
		when(conversationRepository.existsById(conversationId)).thenReturn(true);
		ChatRequestDto request = ChatRequestDto.builder().question("Read this conversation")
				.conversationId(conversationId).build();
		assertThrows(ResourceNotFoundException.class, () -> ragService.askQuestion(request, user));
		verify(conversationRepository, never()).save(any(Conversation.class));
		verifyNoInteractions(vectorStore, chatClient, sessionHistoryMigrationService, conversationHistoryWriter);
	}

	@Test
	void askQuestion_shouldTruncateConversationTitleWhenQuestionExceedsFiftyCharacters() {
		String question = "This is a very long question that exceeds fifty characters";
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubNewConversation();
		stubChatResponse("Generated answer.");
		ChatRequestDto request = ChatRequestDto.builder().question(question).build();
		ragService.askQuestion(request, user);
		ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
		verify(conversationRepository).save(conversationCaptor.capture());
		assertEquals(question.substring(0, 47) + "...", conversationCaptor.getValue().getTitle());
	}

	@Test
	void askQuestion_shouldInitializeSessionBeforeCallingLlm() {
		String conversationId = "session-order-test";
		when(conversationRepository.findByIdAndUser(conversationId, user)).thenReturn(Optional.empty());
		when(conversationRepository.existsById(conversationId)).thenReturn(false);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubChatResponse("Test answer");
		ChatRequestDto request = ChatRequestDto.builder().question("Explain the document")
				.conversationId(conversationId).build();
		ragService.askQuestion(request, user);
		verify(sessionHistoryMigrationService).ensureSessionReady(conversationId, user);
		verify(conversationHistoryWriter).saveUserMessage(conversationId, "Explain the document");
		verify(conversationHistoryWriter).saveAssistantMessage(conversationId, "Test answer");
	}

	// --------------------------------------------------
	// Prompt and advisor tests
	// --------------------------------------------------

	@Test
	void askQuestion_shouldExecuteSystemPromptAndSessionAdvisor() {
		String conversationId = "conversation-123";
		when(conversationRepository.findByIdAndUser(conversationId, user)).thenReturn(Optional.empty());
		when(conversationRepository.existsById(conversationId)).thenReturn(false);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubChatResponse("Test answer");
		ChatRequestDto request = ChatRequestDto.builder().question("Explain the document")
				.conversationId(conversationId).build();
		ragService.askQuestion(request, user);
		verifySystemPromptCallback();
		verifySessionAdvisorInvocation();
	}

	@Test
	void streamQuestionAnswer_shouldExecuteSystemPromptAndSessionAdvisor() {
		String conversationId = "conversation-123";
		when(conversationRepository.findByIdAndUser(conversationId, user)).thenReturn(Optional.empty());
		when(conversationRepository.existsById(conversationId)).thenReturn(false);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
		stubChatStream(Flux.just("Test ", "response"));
		ChatRequestDto request = ChatRequestDto.builder().question("Summarize the document")
				.conversationId(conversationId).build();
		List<String> tokens = ragService.streamQuestionAnswer(request, user).collectList().block();
		assertEquals(List.of("Test ", "response"), tokens);
		verifySystemPromptCallback();
		verifySessionAdvisorInvocation();
	}

	private void verifySystemPromptCallback() {
		ArgumentCaptor<Consumer<ChatClient.PromptSystemSpec>> captor = ArgumentCaptor.captor();
		verify(chatClient.prompt(), times(2)).system(captor.capture());
		ChatClient.PromptSystemSpec systemSpec = mock(ChatClient.PromptSystemSpec.class, RETURNS_SELF);
		Consumer<ChatClient.PromptSystemSpec> actualCallback = captor.getAllValues().get(1);
		actualCallback.accept(systemSpec);
		verify(systemSpec).text(anyString());
		verify(systemSpec).param(eq("doc_context"), eq("No relevant uploaded-document context was retrieved."));
	}

	private void verifySessionAdvisorInvocation() {
		verify(chatClient.prompt().system(ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())
				.user(anyString()), times(2)).advisors(ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any());
	}

	// --------------------------------------------------
	// Retrieval branch tests
	// --------------------------------------------------

	@Test
	void searchSimilarChunks_shouldUseProvidedTopKAndSimilarityThreshold() {
		SearchRequestDto request = SearchRequestDto.builder().query("Search document").topK(20).similaritySearch(0.75)
				.build();
		when(vectorStore.similaritySearch(any(SearchRequest.class)))
				.thenReturn(List.of(new Document("Test document content")));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertNotNull(result);
		assertEquals(1, result.getTotalMatches());
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void searchSimilarChunks_shouldUseDefaultValuesForNonPositiveTopKAndThreshold() {
		SearchRequestDto request = SearchRequestDto.builder().query("Search document").topK(0).similaritySearch(0.0)
				.build();
		when(vectorStore.similaritySearch(any(SearchRequest.class)))
				.thenReturn(List.of(new Document("Test document content")));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertNotNull(result);
		assertEquals(1, result.getTotalMatches());
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void searchSimilarChunks_shouldApplyDocumentSpecificFilter() {
		UUID documentId = UUID.randomUUID();
		DocumentMetadata metadata = mock(DocumentMetadata.class);
		when(documentMetadataRepository.findByIdAndUser(documentId, user)).thenReturn(Optional.of(metadata));
		SearchRequestDto request = SearchRequestDto.builder().query("Search specific document").documentId(documentId)
				.topK(10).similaritySearch(0.70).build();
		when(vectorStore.similaritySearch(any(SearchRequest.class)))
				.thenReturn(List.of(new Document("Test document content")));
		SearchResultDto result = ragService.searchSimilarChunks(request, user);
		assertNotNull(result);
		assertEquals(1, result.getTotalMatches());
		verify(documentMetadataRepository).findByIdAndUser(documentId, user);
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	// --------------------------------------------------
	// Test helpers
	// --------------------------------------------------

	private void stubNewConversation() {
		when(conversationRepository.findByIdAndUser(anyString(), any(User.class))).thenReturn(Optional.empty());
		when(conversationRepository.existsById(anyString())).thenReturn(false);
	}

	private void stubChatResponse(String answer) {
		when(chatClient.prompt().system(ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any()).user(anyString())
				.advisors(ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any()).call().content()).thenReturn(answer);
	}

	private void stubChatStream(Flux<String> response) {
		when(chatClient.prompt().system(ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any()).user(anyString())
				.advisors(ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any()).stream().content()).thenReturn(response);
	}
}
