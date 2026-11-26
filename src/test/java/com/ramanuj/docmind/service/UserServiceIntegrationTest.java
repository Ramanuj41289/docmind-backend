
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ramanuj.docmind.dto.RegisterUserRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.repository.UserRepository;

@DataJpaTest
@Import({ UserService.class, UserServiceIntegrationTest.PasswordConfig.class })
class UserServiceIntegrationTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registerUserShouldPersistUserAndReturnUserDto() {
		RegisterUserRequest request = new RegisterUserRequest("ramanuj", "ramanuj@example.com", "SecurePassword123");

		UserDto result = userService.registerUser(request);

		assertThat(result.id()).isNotNull();
		assertThat(result.username()).isEqualTo("ramanuj");
		assertThat(result.email()).isEqualTo("ramanuj@example.com");
		assertThat(result.role()).isEqualTo(Role.USER);

		User savedUser = userRepository.findById(result.id()).orElseThrow();

		assertThat(savedUser.getUsername()).isEqualTo("ramanuj");
		assertThat(savedUser.getEmail()).isEqualTo("ramanuj@example.com");
		assertThat(savedUser.getRole()).isEqualTo(Role.USER);
	}

	@Test
	void registerUserShouldTrimUsernameAndNormalizeEmail() {
		RegisterUserRequest request = new RegisterUserRequest("  ramanuj  ", "  RAMANUJ@EXAMPLE.COM  ",
				"SecurePassword123");

		UserDto result = userService.registerUser(request);

		assertThat(result.username()).isEqualTo("ramanuj");
		assertThat(result.email()).isEqualTo("ramanuj@example.com");

		assertThat(userRepository.findByUsername("ramanuj")).isPresent();
		assertThat(userRepository.findByEmail("ramanuj@example.com")).isPresent();
	}

	@Test
	void registerUserShouldEncodePasswordBeforePersisting() {
		String rawPassword = "SecurePassword123";

		RegisterUserRequest request = new RegisterUserRequest("passworduser", "password@example.com", rawPassword);

		UserDto result = userService.registerUser(request);

		User savedUser = userRepository.findById(result.id()).orElseThrow();

		assertThat(savedUser.getPassword()).isNotEqualTo(rawPassword);
		assertThat(passwordEncoder.matches(rawPassword, savedUser.getPassword())).isTrue();
	}

	@Test
	void registerUserShouldNotTrimPassword() {
		String rawPassword = "  SecurePassword123  ";

		RegisterUserRequest request = new RegisterUserRequest("spaceuser", "space@example.com", rawPassword);

		UserDto result = userService.registerUser(request);

		User savedUser = userRepository.findById(result.id()).orElseThrow();

		assertThat(passwordEncoder.matches(rawPassword, savedUser.getPassword())).isTrue();

		assertThat(passwordEncoder.matches(rawPassword.trim(), savedUser.getPassword())).isFalse();
	}

	@Test
	void registerUserShouldAlwaysAssignUserRole() {
		RegisterUserRequest request = new RegisterUserRequest("regularuser", "regular@example.com",
				"SecurePassword123");

		UserDto result = userService.registerUser(request);

		User savedUser = userRepository.findById(result.id()).orElseThrow();

		assertThat(result.role()).isEqualTo(Role.USER);
		assertThat(savedUser.getRole()).isEqualTo(Role.USER);
	}

	@Test
	void registerUserShouldRejectDuplicateUsername() {
		saveUser("existinguser", "existing@example.com");

		RegisterUserRequest request = new RegisterUserRequest("existinguser", "different@example.com",
				"SecurePassword123");

		assertThatThrownBy(() -> userService.registerUser(request)).isInstanceOf(ConflictException.class)
				.hasMessage("Username already exists");

		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	void registerUserShouldRejectDuplicateEmail() {
		saveUser("existinguser", "existing@example.com");

		RegisterUserRequest request = new RegisterUserRequest("differentuser", "existing@example.com",
				"SecurePassword123");

		assertThatThrownBy(() -> userService.registerUser(request)).isInstanceOf(ConflictException.class)
				.hasMessage("Email already registered");

		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	void registerUserShouldRejectEmailAlreadyRegisteredWithDifferentCase() {
		saveUser("existinguser", "existing@example.com");

		RegisterUserRequest request = new RegisterUserRequest("differentuser", "EXISTING@EXAMPLE.COM",
				"SecurePassword123");

		assertThatThrownBy(() -> userService.registerUser(request)).isInstanceOf(ConflictException.class)
				.hasMessage("Email already registered");

		assertThat(userRepository.count()).isEqualTo(1);
	}

	private User saveUser(String username, String email) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode("ExistingPassword123"));
		user.setRole(Role.USER);

		return userRepository.saveAndFlush(user);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class PasswordConfig {

		@Bean
		PasswordEncoder passwordEncoder() {
			return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
		}
	}
}
