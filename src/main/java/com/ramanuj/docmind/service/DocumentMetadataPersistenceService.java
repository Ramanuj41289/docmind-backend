
package com.ramanuj.docmind.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.repository.DocumentMetadataRepository;
import com.ramanuj.docmind.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentMetadataPersistenceService {
	private final DocumentMetadataRepository documentMetadataRepository;
	private final UserRepository userRepository;

	@Transactional
	public UUID create(String fileName, String contentType, long fileSize, Long userId) {

		DocumentMetadata metadata = DocumentMetadata.builder().filename(fileName).contentType(contentType)
				.fileSize(fileSize).status(DocumentStatus.UPLOADING).createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now()).user(userRepository.getReferenceById(userId)).build();
		return documentMetadataRepository.save(metadata).getId();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(UUID documentId, String errorMessage) {
		documentMetadataRepository.findById(documentId).ifPresent(metadata -> {
			metadata.setStatus(DocumentStatus.FAILED);
			metadata.setErrorMessage(errorMessage);
			metadata.setUpdatedAt(LocalDateTime.now());
			documentMetadataRepository.save(metadata);
		});
	}
}
