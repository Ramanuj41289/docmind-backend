package com.ramanuj.docmind.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramanuj.docmind.dto.ApiErrorResponse;
import com.ramanuj.docmind.dto.LoginRequestDto;
import com.ramanuj.docmind.dto.LoginResponseDto;
import com.ramanuj.docmind.dto.RegisterUserRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.service.CustomUserDetail;
import com.ramanuj.docmind.service.JwtService;
import com.ramanuj.docmind.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	@Operation(summary = "Authenticate user", description = "Authenticates a user with username and password and returns a JWT access token.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Authentication successful"),
			@ApiResponse(responseCode = "400", description = "Invalid request or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Invalid username or password", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@PostMapping("/login")
	public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));

		CustomUserDetail principal = (CustomUserDetail) authentication.getPrincipal();

		User user = principal.getUser();

		String token = jwtService.generateToken(principal);

		LoginResponseDto response = new LoginResponseDto(token,
				new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole()));

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Register user", description = "Creates a new user account.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "User registered successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid registration data or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@PostMapping("/register")
	public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterUserRequest request) {

		UserDto userDto = userService.registerUser(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
	}
}