
package com.ramanuj.docmind.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.dto.RegisterUserRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserDto registerUser(RegisterUserRequest request) {

		String username = request.username().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);

		if (userRepository.existsByUsername(username)) {
			throw new ConflictException("Username already exists");
		}

		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already registered");
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);

		// Never trim passwords.
		user.setPassword(passwordEncoder.encode(request.password()));

		// Never accept the role from the registration request.
		user.setRole(Role.USER);

		User savedUser = userRepository.save(user);

		return new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
	}
}
