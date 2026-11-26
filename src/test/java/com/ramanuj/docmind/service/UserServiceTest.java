
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ramanuj.docmind.dto.RegisterUserRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterUserRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterUserRequest(
                "  Ramanuj  ",
                "  RAMANUJ@EXAMPLE.COM  ",
                "MyPassword123"
        );
    }

    @Test
    void registerUserShouldSuccessfullyCreateUser() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        UserDto result = userService.registerUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Ramanuj", result.username());
        assertEquals("ramanuj@example.com", result.email());
        assertEquals(Role.USER, result.role());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserShouldTrimUsername() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals("Ramanuj", captor.getValue().getUsername());
    }

    @Test
    void registerUserShouldConvertEmailToLowercase() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals(
                "ramanuj@example.com",
                captor.getValue().getEmail()
        );
    }

    @Test
    void registerUserShouldEncodePassword() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        verify(passwordEncoder).encode("MyPassword123");

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals(
                "encoded-password",
                captor.getValue().getPassword()
        );
    }

    @Test
    void registerUserShouldNotTrimPassword() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        verify(passwordEncoder).encode("MyPassword123");
    }

    @Test
    void registerUserShouldAssignDefaultUserRole() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals(Role.USER, captor.getValue().getRole());
    }

    @Test
    void registerUserShouldRejectDuplicateUsername() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> userService.registerUser(request)
        );

        assertEquals(
                "Username already exists",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void registerUserShouldRejectDuplicateEmail() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> userService.registerUser(request)
        );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void registerUserShouldSaveOnlyOnce() {

        when(userRepository.existsByUsername("Ramanuj"))
                .thenReturn(false);

        when(userRepository.existsByEmail("ramanuj@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MyPassword123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        verify(userRepository).save(any(User.class));
    }
}
