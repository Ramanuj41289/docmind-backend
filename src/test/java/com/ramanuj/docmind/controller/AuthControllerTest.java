
package com.ramanuj.docmind.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramanuj.docmind.dto.LoginRequestDto;
import com.ramanuj.docmind.dto.RegisterUserRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.exception.GlobalExceptionHandler;
import com.ramanuj.docmind.service.CustomUserDetail;
import com.ramanuj.docmind.service.JwtService;
import com.ramanuj.docmind.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private UserService userService;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthController authController;

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	private User user;

	private CustomUserDetail customUserDetail;

	@BeforeEach
	void setUp() {

		objectMapper = new ObjectMapper();

		mockMvc = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(new GlobalExceptionHandler())
				.build();

		user = User.builder().id(1L).username("testuser").email("testuser@example.com").password("encodedPassword")
				.role(Role.USER).build();

		customUserDetail = new CustomUserDetail(user);
	}

	@Test
	void register_shouldReturnCreatedUser() throws Exception {

		RegisterUserRequest request = new RegisterUserRequest("testuser", "testuser@example.com", "Password123");

		UserDto response = new UserDto(1L, "testuser", "testuser@example.com", Role.USER);

		when(userService.registerUser(any(RegisterUserRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.username").value("testuser"))
				.andExpect(jsonPath("$.email").value("testuser@example.com"))
				.andExpect(jsonPath("$.role").value("USER"));

		verify(userService).registerUser(any(RegisterUserRequest.class));
	}

	@Test
	void register_shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {

		RegisterUserRequest request = new RegisterUserRequest("testuser", "testuser@example.com", "Password123");

		when(userService.registerUser(any(RegisterUserRequest.class)))
				.thenThrow(new ConflictException("Username already exists"));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Username already exists"))
				.andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.timestamp").exists());

		verify(userService).registerUser(any(RegisterUserRequest.class));
	}

	@Test
	void register_shouldReturnBadRequestForInvalidRequest() throws Exception {

		String invalidRequest = """
				{
				    "username": "ab",
				    "email": "invalid-email",
				    "password": "123"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data.username").value("Username must be between 3 and 50 characters"))
				.andExpect(jsonPath("$.data.email").value("Invalid email format"))
				.andExpect(jsonPath("$.data.password").value("Password must be between 8 and 72 characters"));

		verifyNoInteractions(userService);
	}

	@Test
	void login_shouldReturnJwtTokenAndUserDetails() throws Exception {

		LoginRequestDto request = new LoginRequestDto("testuser", "Password123");

		Authentication authenticatedToken = new UsernamePasswordAuthenticationToken(customUserDetail, null,
				customUserDetail.getAuthorities());

		when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedToken);

		when(jwtService.generateToken(customUserDetail)).thenReturn("mock-jwt-token");

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("mock-jwt-token")).andExpect(jsonPath("$.user.id").value(1))
				.andExpect(jsonPath("$.user.username").value("testuser"))
				.andExpect(jsonPath("$.user.email").value("testuser@example.com"))
				.andExpect(jsonPath("$.user.role").value("USER"));

		verify(authenticationManager).authenticate(any(Authentication.class));
		verify(jwtService).generateToken(customUserDetail);
	}

	@Test
	void login_shouldTrimUsernameBeforeAuthentication() throws Exception {

		LoginRequestDto request = new LoginRequestDto("  testuser  ", "Password123");

		Authentication authenticatedToken = new UsernamePasswordAuthenticationToken(customUserDetail, null,
				customUserDetail.getAuthorities());

		when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedToken);

		when(jwtService.generateToken(customUserDetail)).thenReturn("mock-jwt-token");

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("mock-jwt-token"));

		ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

		verify(authenticationManager).authenticate(authenticationCaptor.capture());

		UsernamePasswordAuthenticationToken actualToken = (UsernamePasswordAuthenticationToken) authenticationCaptor
				.getValue();

		assertEquals("testuser", actualToken.getPrincipal());
		assertEquals("Password123", actualToken.getCredentials());

		verify(jwtService).generateToken(customUserDetail);
	}

	@Test
	void login_shouldReturnUnauthorizedForInvalidCredentials() throws Exception {

		LoginRequestDto request = new LoginRequestDto("testuser", "WrongPassword");

		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Invalid username or password"))
				.andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.timestamp").exists());

		verify(authenticationManager).authenticate(any(Authentication.class));
		verifyNoInteractions(jwtService);
	}

	@Test
	void login_shouldReturnBadRequestForBlankCredentials() throws Exception {

		String invalidRequest = """
				{
				    "username": " ",
				    "password": ""
				}
				""";

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data.username").value("Username is required"))
				.andExpect(jsonPath("$.data.password").value("Password is required"));

		verifyNoInteractions(authenticationManager, jwtService);
	}
}
