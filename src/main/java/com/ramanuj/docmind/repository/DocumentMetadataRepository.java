package com.ramanuj.docmind.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramanuj.docmind.entity.DocumentMetadata;
import com.ramanuj.docmind.entity.DocumentStatus;
import com.ramanuj.docmind.entity.User;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, UUID> {
	List<DocumentMetadata> findByStatus(DocumentStatus status);
    List<DocumentMetadata> findAllByOrderByCreatedAtDesc();
    List<DocumentMetadata> findByUserOrderByCreatedAtDesc(User user);
    Optional<DocumentMetadata> findByIdAndUser(UUID id, User user);
}
 