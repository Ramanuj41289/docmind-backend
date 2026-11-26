
package com.ramanuj.docmind.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramanuj.docmind.dto.AdminStatusDto;
import com.ramanuj.docmind.dto.UpdateUserRoleRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.exception.GlobalExceptionHandler;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

	@Mock
	private AdminService adminService;

	@InjectMocks
	private AdminController adminController;

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {

		objectMapper = new ObjectMapper();

		mockMvc = MockMvcBuilders.standaloneSetup(adminController).setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void getAllUsers_shouldReturnAllUsers() throws Exception {

		List<UserDto> users = List.of(new UserDto(1L, "admin", "admin@example.com", Role.ADMIN),
				new UserDto(2L, "testuser", "testuser@example.com", Role.USER));

		when(adminService.getAllUsers()).thenReturn(users);

		mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Users retrieved successfully"))
				.andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].username").value("admin"))
				.andExpect(jsonPath("$.data[0].role").value("ADMIN")).andExpect(jsonPath("$.data[1].id").value(2))
				.andExpect(jsonPath("$.data[1].username").value("testuser"))
				.andExpect(jsonPath("$.data[1].role").value("USER")).andExpect(jsonPath("$.timestamp").exists());

		verify(adminService).getAllUsers();
	}

	@Test
	void getAllUsers_shouldReturnEmptyListWhenNoUsersExist() throws Exception {

		when(adminService.getAllUsers()).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.length()").value(0));

		verify(adminService).getAllUsers();
	}

	@Test
	void getUserById_shouldReturnUser() throws Exception {

		UserDto user = new UserDto(2L, "testuser", "testuser@example.com", Role.USER);

		when(adminService.getUserById(2L)).thenReturn(user);

		mockMvc.perform(get("/api/v1/admin/users/{userId}", 2L)).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("User retrieved successfully"))
				.andExpect(jsonPath("$.data.id").value(2)).andExpect(jsonPath("$.data.username").value("testuser"))
				.andExpect(jsonPath("$.data.email").value("testuser@example.com"))
				.andExpect(jsonPath("$.data.role").value("USER")).andExpect(jsonPath("$.timestamp").exists());

		verify(adminService).getUserById(2L);
	}

	@Test
	void getUserById_shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {

		when(adminService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

		mockMvc.perform(get("/api/v1/admin/users/{userId}", 999L)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("User not found with ID: 999"))
				.andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.timestamp").exists());

		verify(adminService).getUserById(999L);
	}

	@Test
	void updateUserRole_shouldPromoteUserToAdmin() throws Exception {

		UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.ADMIN);

		UserDto updatedUser = new UserDto(2L, "testuser", "testuser@example.com", Role.ADMIN);

		when(adminService.updateUserRole(eq(2L), any(UpdateUserRoleRequest.class))).thenReturn(updatedUser);

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", 2L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("User role updated successfully"))
				.andExpect(jsonPath("$.data.id").value(2)).andExpect(jsonPath("$.data.username").value("testuser"))
				.andExpect(jsonPath("$.data.role").value("ADMIN")).andExpect(jsonPath("$.timestamp").exists());

		verify(adminService).updateUserRole(eq(2L), any(UpdateUserRoleRequest.class));
	}

	@Test
	void updateUserRole_shouldReturnConflictWhenDemotingLastAdmin() throws Exception {

		UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.USER);

		when(adminService.updateUserRole(eq(1L), any(UpdateUserRoleRequest.class)))
				.thenThrow(new ConflictException("Cannot demote the last administrator"));

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Cannot demote the last administrator"))
				.andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.timestamp").exists());

		verify(adminService).updateUserRole(eq(1L), any(UpdateUserRoleRequest.class));
	}

	@Test
	void updateUserRole_shouldReturnBadRequestWhenRoleIsMissing() throws Exception {

		String invalidRequest = """
				{
				    "role": null
				}
				""";

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", 2L).contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequest)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data.role").value("Role is required"));

		verifyNoInteractions(adminService);
	}

	@Test
	void getStats_shouldReturnAdminStatistics() throws Exception {

		AdminStatusDto stats = new AdminStatusDto(100L, 10L, 90L);

		when(adminService.getStats()).thenReturn(stats);

		mockMvc.perform(get("/api/v1/admin/status")).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Admin statistics retrieved successfully"))
				.andExpect(jsonPath("$.data.totalUsers").value(100)).andExpect(jsonPath("$.data.totalAdmins").value(10))
				.andExpect(jsonPath("$.data.totalRegularUsers").value(90)).andExpect(jsonPath("$.timestamp").exists());

		verify(adminService).getStats();
	}
}
