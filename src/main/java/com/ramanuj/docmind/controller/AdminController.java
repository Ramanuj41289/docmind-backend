package com.ramanuj.docmind.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ramanuj.docmind.dto.AdminStatusDto;
import com.ramanuj.docmind.dto.ApiErrorResponse;
import com.ramanuj.docmind.dto.UpdateUserRoleRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "Get all users", description = "Retrieves all registered users. Requires administrator privileges.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Administrator access required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@GetMapping("/users")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<List<UserDto>>> getAllUsers() {

		List<UserDto> users = adminService.getAllUsers();

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<List<UserDto>>builder().success(true)
				.message("Users retrieved successfully").data(users).timestamp(LocalDateTime.now()).build());
	}

	@Operation(summary = "Get user by ID", description = "Retrieves a registered user by their identifier. Requires administrator privileges.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Administrator access required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@GetMapping("/users/{userId}")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<UserDto>> getUserById(
			@Parameter(description = "Unique identifier of the user", required = true) @PathVariable Long userId) {

		UserDto user = adminService.getUserById(userId);

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<UserDto>builder().success(true)
				.message("User retrieved successfully").data(user).timestamp(LocalDateTime.now()).build());
	}

	@Operation(summary = "Update user role", description = "Updates the role assigned to a user. Requires administrator privileges.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "User role updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid role or validation failure", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Administrator access required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Conflicting user data", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@PatchMapping("/users/{userId}/role")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<UserDto>> updateUserRole(
			@Parameter(description = "Unique identifier of the user", required = true) @PathVariable Long userId,
			@Valid @RequestBody UpdateUserRoleRequest request) {

		UserDto updatedUser = adminService.updateUserRole(userId, request);

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<UserDto>builder().success(true)
				.message("User role updated successfully").data(updatedUser).timestamp(LocalDateTime.now()).build());
	}

	@Operation(summary = "Get administrator statistics", description = "Retrieves user account statistics grouped by role. Requires administrator privileges.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Administrator statistics retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "Administrator access required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))) })
	@GetMapping("/status")
	public ResponseEntity<com.ramanuj.docmind.dto.ApiResponse<AdminStatusDto>> getStats() {

		AdminStatusDto stats = adminService.getStats();

		return ResponseEntity.ok(com.ramanuj.docmind.dto.ApiResponse.<AdminStatusDto>builder().success(true)
				.message("Admin statistics retrieved successfully").data(stats).timestamp(LocalDateTime.now()).build());
	}
}