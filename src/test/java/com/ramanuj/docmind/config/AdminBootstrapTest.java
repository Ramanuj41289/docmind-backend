
package com.ramanuj.docmind.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AdminBootstrap adminBootstrap;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(adminBootstrap, "username", "");
		ReflectionTestUtils.setField(adminBootstrap, "email", "");
		ReflectionTestUtils.setField(adminBootstrap, "password", "");
	}

	@Test
	void run_shouldSkipWhenBootstrapCredentialsAreNotConfigured() {

		adminBootstrap.run(new DefaultApplicationArguments(new String[0]));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void run_shouldSkipWhenAllBootstrapCredentialsAreNull() {

		ReflectionTestUtils.setField(adminBootstrap, "username", null);
		ReflectionTestUtils.setField(adminBootstrap, "email", null);
		ReflectionTestUtils.setField(adminBootstrap, "password", null);

		adminBootstrap.run(new DefaultApplicationArguments(new String[0]));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void run_shouldRejectIncompleteBootstrapConfiguration() {

		ReflectionTestUtils.setField(adminBootstrap, "username", "admin");

		assertIncompleteConfiguration();

	}

	@Test
	void run_shouldRejectEmailOnlyConfiguration() {

		ReflectionTestUtils.setField(adminBootstrap, "email", "admin@test.com");

		assertIncompleteConfiguration();
	}

	@Test
	void run_shouldRejectPasswordOnlyConfiguration() {

		ReflectionTestUtils.setField(adminBootstrap, "password", "StrongPassword123");

		assertIncompleteConfiguration();
	}

	@Test
	void run_shouldRejectConfigurationWithoutPassword() {

		ReflectionTestUtils.setField(adminBootstrap, "username", "admin");
		ReflectionTestUtils.setField(adminBootstrap, "email", "admin@test.com");

		assertIncompleteConfiguration();
	}

	@Test
	void run_shouldRejectConfigurationWithoutEmail() {

		ReflectionTestUtils.setField(adminBootstrap, "username", "admin");
		ReflectionTestUtils.setField(adminBootstrap, "password", "StrongPassword123");

		assertIncompleteConfiguration();
	}

	@Test
	void run_shouldRejectConfigurationWithoutUsername() {

		ReflectionTestUtils.setField(adminBootstrap, "email", "admin@test.com");
		ReflectionTestUtils.setField(adminBootstrap, "password", "StrongPassword123");

		assertIncompleteConfiguration();
	}

	@Test
	void run_shouldRejectPasswordShorterThanTwelveCharacters() {

		configureCredentials("admin", "admin@test.com", "shortpass");

		assertThatThrownBy(() -> adminBootstrap.run(new DefaultApplicationArguments(new String[0])))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Initial admin password must contain at least 12 characters");

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void run_shouldSkipWhenMatchingAdminAlreadyExists() {

		configureCredentials("admin", " ADMIN@TEST.COM ", "StrongPassword123");

		User existingAdmin = User.builder().username("admin").email("admin@test.com")
				.password("existing-encoded-password").role(Role.ADMIN).build();

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));

		adminBootstrap.run(new DefaultApplicationArguments(new String[0]));

		verify(userRepository).findByUsername("admin");
		verify(userRepository, never()).existsByEmail("admin@test.com");
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void run_shouldRejectUsernameAlreadyBelongingToAnotherAccount() {

		configureCredentials("admin", "admin@test.com", "StrongPassword123");

		User existingUser = User.builder().username("admin").email("user@test.com").password("encoded-password")
				.role(Role.USER).build();

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

		assertThatThrownBy(() -> adminBootstrap.run(new DefaultApplicationArguments(new String[0])))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Bootstrap username already belongs to an existing account. "
						+ "No role or account details were changed.");

		verify(userRepository).findByUsername("admin");
		verify(userRepository, never()).existsByEmail("admin@test.com");
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void run_shouldRejectEmailAlreadyUsedByAnotherAccount() {

		configureCredentials("newadmin", "admin@test.com", "StrongPassword123");

		when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());

		when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

		assertThatThrownBy(() -> adminBootstrap.run(new DefaultApplicationArguments(new String[0])))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Bootstrap email already belongs to another account. " + "No account was created.");

		verify(userRepository).findByUsername("newadmin");
		verify(userRepository).existsByEmail("admin@test.com");
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void run_shouldCreateAdminWithNormalizedCredentialsAndEncodedPassword() {

		configureCredentials("  newadmin  ", "  NEWADMIN@TEST.COM  ", "StrongPassword123");

		when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());

		when(userRepository.existsByEmail("newadmin@test.com")).thenReturn(false);

		when(passwordEncoder.encode("StrongPassword123")).thenReturn("encoded-strong-password");

		adminBootstrap.run(new DefaultApplicationArguments(new String[0]));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());
		verify(passwordEncoder).encode("StrongPassword123");

		User savedUser = userCaptor.getValue();

		assertThat(savedUser.getUsername()).isEqualTo("newadmin");
		assertThat(savedUser.getEmail()).isEqualTo("newadmin@test.com");
		assertThat(savedUser.getPassword()).isEqualTo("encoded-strong-password");
		assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
	}

	private void assertIncompleteConfiguration() {

		assertThatThrownBy(() -> adminBootstrap.run(new DefaultApplicationArguments(new String[0])))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Admin bootstrap requires username, email and password");

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	private void configureCredentials(String username, String email, String password) {

		ReflectionTestUtils.setField(adminBootstrap, "username", username);
		ReflectionTestUtils.setField(adminBootstrap, "email", email);
		ReflectionTestUtils.setField(adminBootstrap, "password", password);
	}
}
