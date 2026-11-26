
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailService customUserDetailService;

	private User user;

	@BeforeEach
	void setUp() {
		user = User.builder().id(1L).username("testuser").password("encoded-password").email("test@example.com")
				.role(Role.USER).build();
	}

	@Test
	void loadUserByUsername_shouldReturnUserDetails() {

		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailService.loadUserByUsername("testuser");

		assertInstanceOf(CustomUserDetail.class, userDetails);

		CustomUserDetail customUserDetail = (CustomUserDetail) userDetails;

		assertEquals(user, customUserDetail.getUser());
		assertEquals("testuser", userDetails.getUsername());
		assertEquals("encoded-password", userDetails.getPassword());

		verify(userRepository).findByUsername("testuser");
	}

	@Test
	void loadUserByUsername_shouldAssignUserRoleAuthority() {

		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailService.loadUserByUsername("testuser");

		assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

		assertEquals(1, userDetails.getAuthorities().size());

		verify(userRepository).findByUsername("testuser");
	}

	@Test
	void loadUserByUsername_shouldAssignAdminRoleAuthority() {

		User adminUser = User.builder().id(2L).username("admin").password("encoded-admin-password")
				.email("admin@example.com").role(Role.ADMIN).build();

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

		UserDetails userDetails = customUserDetailService.loadUserByUsername("admin");

		assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

		assertEquals(1, userDetails.getAuthorities().size());

		verify(userRepository).findByUsername("admin");
	}

	@Test
	void loadUserByUsername_shouldThrowExceptionWhenUserDoesNotExist() {

		when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

		UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
				() -> customUserDetailService.loadUserByUsername("unknown"));

		assertEquals("Invalid username or password", exception.getMessage());

		verify(userRepository).findByUsername("unknown");
	}

	@Test
	void loadUserByUsername_shouldReturnAccountStatusAsEnabled() {

		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailService.loadUserByUsername("testuser");

		assertTrue(userDetails.isEnabled());
		assertTrue(userDetails.isAccountNonExpired());
		assertTrue(userDetails.isAccountNonLocked());
		assertTrue(userDetails.isCredentialsNonExpired());

		verify(userRepository).findByUsername("testuser");
	}
}
