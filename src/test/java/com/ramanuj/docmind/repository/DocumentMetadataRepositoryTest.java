
package com.ramanuj.docmind.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;

@DataJpaTest
class DocumentMetadataRepositoryTest {

	@Autowired
	private DocumentMetadataRepository documentMetadataRepository;

	@Autowired
	private TestEntityManager entityManager;

	private long timestampSequence = 0;

	@Test
	void findByStatus_shouldReturnDocumentsWithMatchingStatus() {
		User user = createUser("statususer");

		createDocument(user, "indexed.pdf", DocumentStatus.INDEXED);
		createDocument(user, "failed.pdf", DocumentStatus.FAILED);
		createDocument(user, "another-indexed.pdf", DocumentStatus.INDEXED);

		List<DocumentMetadata> result = documentMetadataRepository.findByStatus(DocumentStatus.INDEXED);

		assertThat(result).hasSize(2);
		assertThat(result).allSatisfy(document -> assertThat(document.getStatus()).isEqualTo(DocumentStatus.INDEXED));
	}

	@Test
	void findByStatus_shouldReturnEmptyListWhenNoMatchingStatusExists() {
		User user = createUser("nostatususer");

		createDocument(user, "failed.pdf", DocumentStatus.FAILED);

		List<DocumentMetadata> result = documentMetadataRepository.findByStatus(DocumentStatus.INDEXED);

		assertThat(result).isEmpty();
	}

	@Test
	void findAllByOrderByCreatedAtDesc_shouldReturnDocumentsInDescendingOrder() {
		User user = createUser("orderuser");

		DocumentMetadata older = createDocument(user, "older.pdf", DocumentStatus.INDEXED);

		DocumentMetadata newer = createDocument(user, "newer.pdf", DocumentStatus.PROCESSING);

		List<DocumentMetadata> result = documentMetadataRepository.findAllByOrderByCreatedAtDesc();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(newer.getId());
		assertThat(result.get(1).getId()).isEqualTo(older.getId());
	}

	@Test
	void findAllByOrderByCreatedAtDesc_shouldReturnEmptyListWhenNoDocumentsExist() {
		List<DocumentMetadata> result = documentMetadataRepository.findAllByOrderByCreatedAtDesc();

		assertThat(result).isEmpty();
	}

	@Test
	void findByUserOrderByCreatedAtDesc_shouldReturnOnlyUsersDocumentsInDescendingOrder() {
		User user = createUser("owneruser");
		User anotherUser = createUser("anotherowner");

		DocumentMetadata older = createDocument(user, "older.pdf", DocumentStatus.INDEXED);

		DocumentMetadata newer = createDocument(user, "newer.pdf", DocumentStatus.PROCESSING);

		createDocument(anotherUser, "other.pdf", DocumentStatus.INDEXED);

		List<DocumentMetadata> result = documentMetadataRepository.findByUserOrderByCreatedAtDesc(user);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(newer.getId());
		assertThat(result.get(1).getId()).isEqualTo(older.getId());
		assertThat(result).allSatisfy(document -> assertThat(document.getUser().getId()).isEqualTo(user.getId()));
	}

	@Test
	void findByUserOrderByCreatedAtDesc_shouldReturnEmptyListWhenUserHasNoDocuments() {
		User user = createUser("emptyuser");
		User anotherUser = createUser("documentowner");

		createDocument(anotherUser, "document.pdf", DocumentStatus.INDEXED);

		List<DocumentMetadata> result = documentMetadataRepository.findByUserOrderByCreatedAtDesc(user);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdAndUser_shouldReturnDocumentWhenIdAndOwnerMatch() {
		User user = createUser("matchingowner");

		DocumentMetadata document = createDocument(user, "matching.pdf", DocumentStatus.INDEXED);

		Optional<DocumentMetadata> result = documentMetadataRepository.findByIdAndUser(document.getId(), user);

		assertThat(result).isPresent();
		assertThat(result.get().getFilename()).isEqualTo("matching.pdf");
		assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
	}

	@Test
	void findByIdAndUser_shouldReturnEmptyWhenOwnerDoesNotMatch() {
		User owner = createUser("actualowner");
		User anotherUser = createUser("differentowner");

		DocumentMetadata document = createDocument(owner, "private.pdf", DocumentStatus.INDEXED);

		Optional<DocumentMetadata> result = documentMetadataRepository.findByIdAndUser(document.getId(), anotherUser);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdAndUser_shouldReturnEmptyWhenDocumentDoesNotExist() {
		User user = createUser("missingdocuser");

		Optional<DocumentMetadata> result = documentMetadataRepository.findByIdAndUser(UUID.randomUUID(), user);

		assertThat(result).isEmpty();
	}

	@Test
	void save_shouldGenerateUuidAndPersistDocument() {
		User user = createUser("saveuser");

		DocumentMetadata document = DocumentMetadata.builder().filename("saved-document.pdf")
				.contentType("application/pdf").fileSize(1024L).totalPages(5).totalChunks(10)
				.status(DocumentStatus.UPLOADING).createdAt(LocalDateTime.now()).user(user).build();

		DocumentMetadata saved = documentMetadataRepository.saveAndFlush(document);

		assertThat(saved.getId()).isNotNull();

		Optional<DocumentMetadata> retrieved = documentMetadataRepository.findById(saved.getId());

		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getFilename()).isEqualTo("saved-document.pdf");
		assertThat(retrieved.get().getStatus()).isEqualTo(DocumentStatus.UPLOADING);
	}

	@Test
	void deleteById_shouldRemoveDocument() {
		User user = createUser("deletebyiduser");

		DocumentMetadata document = createDocument(user, "delete-by-id.pdf", DocumentStatus.INDEXED);

		UUID documentId = document.getId();

		assertThat(documentMetadataRepository.existsById(documentId)).isTrue();

		documentMetadataRepository.deleteById(documentId);
		documentMetadataRepository.flush();

		assertThat(documentMetadataRepository.existsById(documentId)).isFalse();
		assertThat(documentMetadataRepository.findById(documentId)).isEmpty();
	}

	@Test
	void delete_shouldRemoveDocumentEntity() {
		User user = createUser("deleteentityuser");

		DocumentMetadata document = createDocument(user, "delete-entity.pdf", DocumentStatus.INDEXED);

		UUID documentId = document.getId();

		documentMetadataRepository.delete(document);
		documentMetadataRepository.flush();

		assertThat(documentMetadataRepository.findById(documentId)).isEmpty();
	}

	@Test
	void existsById_shouldReturnTrueForExistingDocument() {
		User user = createUser("existsuser");

		DocumentMetadata document = createDocument(user, "exists.pdf", DocumentStatus.INDEXED);

		assertThat(documentMetadataRepository.existsById(document.getId())).isTrue();
	}

	@Test
	void count_shouldReturnTotalNumberOfDocuments() {
		User user = createUser("countuser");

		createDocument(user, "first.pdf", DocumentStatus.INDEXED);
		createDocument(user, "second.pdf", DocumentStatus.PROCESSING);
		createDocument(user, "third.pdf", DocumentStatus.FAILED);

		assertThat(documentMetadataRepository.count()).isEqualTo(3);
	}

	private User createUser(String username) {
		User user = User.builder().username(username).password("encoded-password").email(username + "@test.com")
				.role(Role.USER).build();

		return entityManager.persistAndFlush(user);
	}

	private DocumentMetadata createDocument(User user, String filename, DocumentStatus status) {

		DocumentMetadata document = DocumentMetadata.builder().filename(filename).contentType("application/pdf")
				.fileSize(2048L).totalPages(5).totalChunks(10).status(status)
				.createdAt(LocalDateTime.now().plusSeconds(timestampSequence++)).user(user).build();

		return entityManager.persistAndFlush(document);
	}
}
