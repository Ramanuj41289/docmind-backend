
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.dto.ChatRequestDto;
import com.ramanuj.docmind.dto.CitationDto;
import com.ramanuj.docmind.dto.SearchRequestDto;
import com.ramanuj.docmind.dto.SearchResultDto;
import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.exception.VectorSearchException;
import com.ramanuj.docmind.repository.ConversationRepository;

@DataJpaTest
@Import({ RagService.class, AppProperties.class })
class RagServiceIntegrationTest {

	@Autowired
	private RagService ragService;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ConversationRepository conversationRepository;

	@MockitoBean
	private VectorStore vectorStore;

	@MockitoBean
	private ChatClient chatClient;

	@MockitoBean
	private SessionHistoryMigrationService sessionHistoryMigrationService;

	@MockitoBean
	private ConversationHistoryWriter conversationHistoryWriter;

	private User user;
	private User anotherUser;
	private DocumentMetadata documentMetadata;

	@BeforeEach
	void setUp() {

		user = User.builder().username("raguser").email("raguser@example.com").password("encoded-password")
				.role(Role.USER).build();

		anotherUser = User.builder().username("anotheruser").email("anotheruser@example.com")
				.password("encoded-password").role(Role.USER).build();

		entityManager.persistAndFlush(user);
		entityManager.persistAndFlush(anotherUser);

		documentMetadata = DocumentMetadata.builder().filename("annual-report.pdf").contentType("application/pdf")
				.fileSize(102400L).totalPages(10).totalChunks(5).status(DocumentStatus.INDEXED)
				.createdAt(LocalDateTime.now()).user(user).build();

		entityManager.persistAndFlush(documentMetadata);
	}

	// --------------------------------------------------
	// Similarity search tests
	// --------------------------------------------------

	@Test
	void searchSimilarChunksShouldReturnMappedCitations() {

		UUID documentId = documentMetadata.getId();

		Document retrievedDocument = new Document("Revenue increased by 15% during the financial year.",
				Map.of("documentId", documentId.toString(), "fileName", "annual-report.pdf", "chunkIndex", 2,
						"pageNumber", 4, "distance", 0.15, "userId", user.getId().toString()));

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(retrievedDocument));

		SearchRequestDto request = SearchRequestDto.builder().query("What was the revenue growth?")
				.documentId(documentId).topK(5).similaritySearch(0.7).build();

		SearchResultDto result = ragService.searchSimilarChunks(request, user);

		assertThat(result.getQuery()).isEqualTo("What was the revenue growth?");
		assertThat(result.getTotalMatches()).isEqualTo(1);
		assertThat(result.getMatches()).hasSize(1);

		CitationDto citation = result.getMatches().get(0);

		assertThat(citation.getDocumentId()).isEqualTo(documentId);
		assertThat(citation.getFileName()).isEqualTo("annual-report.pdf");
		assertThat(citation.getChunkIndex()).isEqualTo(2);
		assertThat(citation.getPageNumber()).isEqualTo(4);
		assertThat(citation.getSnippet()).isEqualTo("Revenue increased by 15% during the financial year.");
		assertThat(citation.getSimilarityScore()).isEqualTo(0.85);
	}

	@Test
	void searchSimilarChunksShouldReturnEmptyResultsWhenNoDocumentsMatch() {

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		SearchRequestDto request = SearchRequestDto.builder().query("Unknown information").build();

		SearchResultDto result = ragService.searchSimilarChunks(request, user);

		assertThat(result.getQuery()).isEqualTo("Unknown information");
		assertThat(result.getTotalMatches()).isZero();
		assertThat(result.getMatches()).isEmpty();
	}

	@Test
	void searchSimilarChunksShouldRejectNullUser() {

		SearchRequestDto request = SearchRequestDto.builder().query("Revenue growth").build();

		assertThatThrownBy(() -> ragService.searchSimilarChunks(request, null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");
	}

	@Test
	void searchSimilarChunksShouldRejectUserWithoutDatabaseId() {

		User unsavedUser = User.builder().username("unsaveduser").email("unsaveduser@example.com")
				.password("encoded-password").role(Role.USER).build();

		SearchRequestDto request = SearchRequestDto.builder().query("Revenue growth").build();

		assertThatThrownBy(() -> ragService.searchSimilarChunks(request, unsavedUser))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Authenticated user is required");
	}

	@Test
	void searchSimilarChunksShouldRejectDocumentOwnedByAnotherUser() {

		UUID documentId = documentMetadata.getId();

		SearchRequestDto request = SearchRequestDto.builder().query("Revenue growth").documentId(documentId).build();

		assertThatThrownBy(() -> ragService.searchSimilarChunks(request, anotherUser))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found");
	}

	@Test
	void searchSimilarChunksShouldRejectUnknownDocument() {

		SearchRequestDto request = SearchRequestDto.builder().query("Revenue growth").documentId(UUID.randomUUID())
				.build();

		assertThatThrownBy(() -> ragService.searchSimilarChunks(request, user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found");
	}

	@Test
	void searchSimilarChunksShouldWrapVectorStoreFailure() {

		when(vectorStore.similaritySearch(any(SearchRequest.class)))
				.thenThrow(new RuntimeException("Vector database unavailable"));

		SearchRequestDto request = SearchRequestDto.builder().query("Revenue growth").build();

		assertThatThrownBy(() -> ragService.searchSimilarChunks(request, user))
				.isInstanceOf(VectorSearchException.class).hasMessage("Vector search failed")
				.hasCauseInstanceOf(RuntimeException.class);
	}

	@Test
	void searchSimilarChunksShouldHandleInvalidDocumentIdInVectorMetadata() {

		Document retrievedDocument = new Document("The company reported strong financial performance.",
				Map.of("documentId", "invalid-uuid", "fileName", "annual-report.pdf", "chunkIndex", 1, "page_number", 3,
						"distance", 0.2));

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(retrievedDocument));

		SearchRequestDto request = SearchRequestDto.builder().query("Financial performance").build();

		SearchResultDto result = ragService.searchSimilarChunks(request, user);

		assertThat(result.getTotalMatches()).isEqualTo(1);

		CitationDto citation = result.getMatches().get(0);

		assertThat(citation.getDocumentId()).isNull();
		assertThat(citation.getPageNumber()).isEqualTo(3);
		assertThat(citation.getSimilarityScore()).isEqualTo(0.8);
	}

	// --------------------------------------------------
	// Non-streaming chat tests
	// --------------------------------------------------

	@Test
	void askQuestionShouldCreateConversationWhenConversationIdIsMissing() {

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		when(chatClient.prompt()).thenThrow(new RuntimeException("Mocked ChatClient boundary"));

		ChatRequestDto request = ChatRequestDto.builder().question("Explain the annual revenue").build();

		assertThatThrownBy(() -> ragService.askQuestion(request, user)).isInstanceOf(RuntimeException.class)
				.hasMessage("Mocked ChatClient boundary");

		List<Conversation> conversations = conversationRepository.findByUserOrderByUpdatedAtDesc(user);

		assertThat(conversations).hasSize(1);
		assertThat(conversations.get(0).getId()).isNotBlank();
		assertThat(conversations.get(0).getTitle()).isEqualTo("Explain the annual revenue");

		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void askQuestionShouldReuseExistingConversation() {

		Conversation existingConversation = Conversation.builder().id("existing-conversation")
				.title("Existing discussion").user(user).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.build();

		entityManager.persistAndFlush(existingConversation);

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		when(chatClient.prompt()).thenThrow(new RuntimeException("Mocked ChatClient boundary"));

		ChatRequestDto request = ChatRequestDto.builder().conversationId(existingConversation.getId())
				.question("Continue the discussion").build();

		assertThatThrownBy(() -> ragService.askQuestion(request, user)).isInstanceOf(RuntimeException.class)
				.hasMessage("Mocked ChatClient boundary");

		assertThat(conversationRepository.findByUserOrderByUpdatedAtDesc(user)).hasSize(1);
	}

	@Test
	void askQuestionShouldRejectConversationOwnedByAnotherUser() {

		Conversation anotherUsersConversation = Conversation.builder().id("another-users-conversation")
				.title("Private discussion").user(anotherUser).createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now()).build();

		entityManager.persistAndFlush(anotherUsersConversation);

		ChatRequestDto request = ChatRequestDto.builder().conversationId(anotherUsersConversation.getId())
				.question("Access another user's conversation").build();

		assertThatThrownBy(() -> ragService.askQuestion(request, user)).isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Conversation not found");
	}

	@Test
	void askQuestionShouldTruncateLongConversationTitle() {

		String longQuestion = "Explain the complete financial performance, revenue growth, "
				+ "operational expenses and business outlook for the company";

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		when(chatClient.prompt()).thenThrow(new RuntimeException("Mocked ChatClient boundary"));

		ChatRequestDto request = ChatRequestDto.builder().question(longQuestion).build();

		assertThatThrownBy(() -> ragService.askQuestion(request, user)).isInstanceOf(RuntimeException.class)
				.hasMessage("Mocked ChatClient boundary");

		Conversation savedConversation = conversationRepository.findByUserOrderByUpdatedAtDesc(user).get(0);

		assertThat(savedConversation.getTitle()).isEqualTo(longQuestion.substring(0, 47) + "...");
	}

	// --------------------------------------------------
	// Streaming chat tests
	// --------------------------------------------------

	@Test
	void streamQuestionAnswerShouldCreateConversationWhenIdIsMissing() {

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		when(chatClient.prompt()).thenThrow(new RuntimeException("Mocked ChatClient boundary"));

		ChatRequestDto request = ChatRequestDto.builder().question("Summarize the document").build();

		assertThatThrownBy(() -> ragService.streamQuestionAnswer(request, user).blockFirst())
				.isInstanceOf(RuntimeException.class).hasMessage("Mocked ChatClient boundary");

		List<Conversation> conversations = conversationRepository.findByUserOrderByUpdatedAtDesc(user);

		assertThat(conversations).hasSize(1);
		assertThat(conversations.get(0).getId()).isNotBlank();
		assertThat(conversations.get(0).getTitle()).isEqualTo("Summarize the document");
	}

	@Test
	void streamQuestionAnswerShouldReuseExistingConversation() {

		Conversation existingConversation = Conversation.builder().id("stream-conversation")
				.title("Streaming discussion").user(user).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.build();

		entityManager.persistAndFlush(existingConversation);

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		when(chatClient.prompt()).thenThrow(new RuntimeException("Mocked ChatClient boundary"));

		ChatRequestDto request = ChatRequestDto.builder().conversationId(existingConversation.getId())
				.question("Continue streaming discussion").build();

		assertThatThrownBy(() -> ragService.streamQuestionAnswer(request, user).blockFirst())
				.isInstanceOf(RuntimeException.class).hasMessage("Mocked ChatClient boundary");

		assertThat(conversationRepository.findByUserOrderByUpdatedAtDesc(user)).hasSize(1);
	}

	@Test
	void streamQuestionAnswerShouldRejectConversationOwnedByAnotherUser() {

		Conversation anotherUsersConversation = Conversation.builder().id("private-stream-conversation")
				.title("Private conversation").user(anotherUser).createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now()).build();

		entityManager.persistAndFlush(anotherUsersConversation);

		ChatRequestDto request = ChatRequestDto.builder().conversationId(anotherUsersConversation.getId())
				.question("Access private conversation").build();

		assertThatThrownBy(() -> ragService.streamQuestionAnswer(request, user))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Conversation not found");
	}
}
