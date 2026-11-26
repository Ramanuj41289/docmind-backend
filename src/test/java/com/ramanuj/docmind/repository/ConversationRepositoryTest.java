
package com.ramanuj.docmind.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.ramanuj.docmind.entity.Conversation;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;

@DataJpaTest
class ConversationRepositoryTest {

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByUserOrderByUpdatedAtDesc_shouldReturnConversationsInDescendingOrder() {

		User user = saveUser("ramanuj");

		Conversation older = saveConversation("conversation-1", user, "Older conversation",
				LocalDateTime.now().minusDays(2));

		Conversation newer = saveConversation("conversation-2", user, "Newer conversation", LocalDateTime.now());

		List<Conversation> result = conversationRepository.findByUserOrderByUpdatedAtDesc(user);

		assertThat(result).hasSize(2);

		assertThat(result.get(0).getId()).isEqualTo(newer.getId());
		assertThat(result.get(1).getId()).isEqualTo(older.getId());
	}

	@Test
	void findByUserOrderByUpdatedAtDesc_shouldReturnOnlyUserConversations() {

		User firstUser = saveUser("ramanuj");
		User secondUser = saveUser("anotheruser");

		saveConversation("conversation-1", firstUser, "First user's conversation", LocalDateTime.now());

		saveConversation("conversation-2", secondUser, "Second user's conversation", LocalDateTime.now());

		List<Conversation> result = conversationRepository.findByUserOrderByUpdatedAtDesc(firstUser);

		assertThat(result).hasSize(1)
				.allMatch(conversation -> conversation.getUser().getId().equals(firstUser.getId()));
	}

	@Test
	void findByIdAndUser_shouldReturnConversationWhenOwnerMatches() {

		User user = saveUser("ramanuj");

		saveConversation("conversation-1", user, "Test conversation", LocalDateTime.now());

		Optional<Conversation> result = conversationRepository.findByIdAndUser("conversation-1", user);

		assertThat(result).isPresent();
		assertThat(result.get().getTitle()).isEqualTo("Test conversation");
	}

	@Test
	void findByIdAndUser_shouldReturnEmptyWhenOwnerDoesNotMatch() {

		User owner = saveUser("ramanuj");
		User anotherUser = saveUser("anotheruser");

		saveConversation("conversation-1", owner, "Private conversation", LocalDateTime.now());

		Optional<Conversation> result = conversationRepository.findByIdAndUser("conversation-1", anotherUser);

		assertThat(result).isEmpty();
	}

	@Test
	void deleteByIdAndUser_shouldDeleteConversationWhenOwnerMatches() {

		User user = saveUser("ramanuj");

		saveConversation("conversation-1", user, "Conversation to delete", LocalDateTime.now());

		conversationRepository.deleteByIdAndUser("conversation-1", user);

		assertThat(conversationRepository.findById("conversation-1")).isEmpty();
	}

	@Test
	void deleteByIdAndUser_shouldNotDeleteConversationWhenOwnerDoesNotMatch() {

		User owner = saveUser("ramanuj");
		User anotherUser = saveUser("anotheruser");

		saveConversation("conversation-1", owner, "Protected conversation", LocalDateTime.now());

		conversationRepository.deleteByIdAndUser("conversation-1", anotherUser);

		assertThat(conversationRepository.findById("conversation-1")).isPresent();
	}

	private User saveUser(String username) {

		User user = User.builder().username(username).email(username + "@test.com").password("encoded-password")
				.role(Role.USER).build();

		return userRepository.save(user);
	}

	private Conversation saveConversation(String id, User user, String title, LocalDateTime updatedAt) {

		Conversation conversation = Conversation.builder().id(id).user(user).title(title)
				.createdAt(updatedAt.minusHours(1)).updatedAt(updatedAt).build();

		return conversationRepository.save(conversation);
	}
}
