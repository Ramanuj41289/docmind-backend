
package com.ramanuj.docmind.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;

@DataJpaTest
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void findByUsername_shouldReturnUserWhenUsernameExists() {

		User user = createUser("ramanuj", "ramanuj@test.com");

		userRepository.save(user);

		Optional<User> result = userRepository.findByUsername("ramanuj");

		assertThat(result).isPresent();
		assertThat(result.get().getUsername()).isEqualTo("ramanuj");
		assertThat(result.get().getEmail()).isEqualTo("ramanuj@test.com");
	}

	@Test
	void findByUsername_shouldReturnEmptyWhenUsernameDoesNotExist() {

		Optional<User> result = userRepository.findByUsername("unknown");

		assertThat(result).isEmpty();
	}

	@Test
	void findByEmail_shouldReturnUserWhenEmailExists() {

		User user = createUser("ramanuj", "ramanuj@test.com");

		userRepository.save(user);

		Optional<User> result = userRepository.findByEmail("ramanuj@test.com");

		assertThat(result).isPresent();
		assertThat(result.get().getUsername()).isEqualTo("ramanuj");
	}

	@Test
	void findByEmail_shouldReturnEmptyWhenEmailDoesNotExist() {

		Optional<User> result = userRepository.findByEmail("unknown@test.com");

		assertThat(result).isEmpty();
	}

	@Test
	void existsByUsername_shouldReturnTrueWhenUsernameExists() {

		userRepository.save(createUser("ramanuj", "ramanuj@test.com"));

		assertThat(userRepository.existsByUsername("ramanuj")).isTrue();
	}

	@Test
	void existsByUsername_shouldReturnFalseWhenUsernameDoesNotExist() {

		assertThat(userRepository.existsByUsername("unknown")).isFalse();
	}

	@Test
	void existsByEmail_shouldReturnTrueWhenEmailExists() {

		userRepository.save(createUser("ramanuj", "ramanuj@test.com"));

		assertThat(userRepository.existsByEmail("ramanuj@test.com")).isTrue();
	}

	@Test
	void existsByEmail_shouldReturnFalseWhenEmailDoesNotExist() {

		assertThat(userRepository.existsByEmail("unknown@test.com")).isFalse();
	}

	@Test
	void countByRole_shouldReturnCorrectCountForEachRole() {

		userRepository.save(createUser("user1", "user1@test.com"));

		userRepository.save(createUser("user2", "user2@test.com"));

		userRepository.save(createAdmin("admin1", "admin1@test.com"));

		assertThat(userRepository.countByRole(Role.USER)).isEqualTo(2);

		assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
	}

	@Test
	void save_shouldPersistUserAndGenerateId() {

		User savedUser = userRepository.save(createUser("ramanuj", "ramanuj@test.com"));

		assertThat(savedUser.getId()).isNotNull();

		Optional<User> result = userRepository.findById(savedUser.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getUsername()).isEqualTo("ramanuj");
	}

	private User createUser(String username, String email) {

		return User.builder().username(username).email(email).password("encoded-password").role(Role.USER).build();
	}

	private User createAdmin(String username, String email) {

		return User.builder().username(username).email(email).password("encoded-password").role(Role.ADMIN).build();
	}
}
