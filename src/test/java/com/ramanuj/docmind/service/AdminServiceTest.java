
package com.ramanuj.docmind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.dto.AdminStatusDto;
import com.ramanuj.docmind.dto.UpdateUserRoleRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppProperties appProperties;

    private AdminService adminService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
		adminService = new AdminService(userRepository, appProperties);

        admin = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .password("encoded-password")
                .role(Role.ADMIN)
                .build();

        regularUser = User.builder()
                .id(2L)
                .username("user")
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Test
    void getAllUsersShouldReturnAllUsers() {
        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(admin, regularUser));

        List<UserDto> result = adminService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("admin", result.get(0).username());
        assertEquals(Role.ADMIN, result.get(0).role());
        assertEquals("user", result.get(1).username());
        assertEquals(Role.USER, result.get(1).role());

        verify(userRepository).findAll(
                Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void getUserByIdShouldReturnUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        UserDto result = adminService.getUserById(1L);

        assertEquals(1L, result.id());
        assertEquals("admin", result.username());
        assertEquals("admin@example.com", result.email());
        assertEquals(Role.ADMIN, result.role());
    }

    @Test
    void getUserByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.getUserById(99L));
    }

    @Test
    void updateUserRoleShouldPromoteUserToAdmin() {
		stubHitl(false);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(regularUser));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = adminService.updateUserRole(
                2L, new UpdateUserRoleRequest(Role.ADMIN));

        assertEquals(Role.ADMIN, result.role());
        verify(userRepository).save(regularUser);
    }

    @Test
    void updateUserRoleShouldNotSaveWhenRoleIsUnchanged() {
		stubHitl(false);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(regularUser));

        UserDto result = adminService.updateUserRole(
                2L, new UpdateUserRoleRequest(Role.USER));

        assertEquals(Role.USER, result.role());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoleShouldPreventDemotionOfLastAdmin() {
		stubHitl(false);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.countByRole(Role.ADMIN))
                .thenReturn(1L);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> adminService.updateUserRole(
                        1L, new UpdateUserRoleRequest(Role.USER)));

        assertEquals("Cannot demote the last administrator",
                exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoleShouldAllowDemotionWhenMultipleAdminsExist() {
		stubHitl(false);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.countByRole(Role.ADMIN))
                .thenReturn(2L);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = adminService.updateUserRole(
                1L, new UpdateUserRoleRequest(Role.USER));

        assertEquals(Role.USER, result.role());
        verify(userRepository).save(admin);
    }

    @Test
    void updateUserRoleShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.updateUserRole(
                        99L, new UpdateUserRoleRequest(Role.ADMIN)));

        verify(userRepository, never()).save(any(User.class));
    }

  @Test
  void updateUserRoleShouldRequireApprovalForHighRiskChangeWhenEnabled() {
    stubHitl(true);

    when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

    ConflictException exception = assertThrows(ConflictException.class,
        () -> adminService.updateUserRole(2L, new UpdateUserRoleRequest(Role.ADMIN, false)));

    assertEquals("Approval is required for high-risk role changes", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateUserRoleShouldAllowHighRiskChangeWhenApproved() {
    stubHitl(true);

    when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserDto result = adminService.updateUserRole(2L, new UpdateUserRoleRequest(Role.ADMIN, true));
    assertEquals(Role.ADMIN, result.role());
    verify(userRepository).save(regularUser);
  }

    @Test
    void getStatsShouldReturnCorrectCounts() {
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userRepository.countByRole(Role.USER)).thenReturn(3L);

        AdminStatusDto result = adminService.getStats();

        assertEquals(5L, result.totalUsers());
        assertEquals(2L, result.totalAdmins());
        assertEquals(3L, result.totalRegularUsers());
    }

  private void stubHitl(boolean enabled) {
    AppProperties.HitlProperties hitlProperties = new AppProperties.HitlProperties();
    hitlProperties.setRequireAdminRoleUpdateApproval(enabled);
    when(appProperties.getHitl()).thenReturn(hitlProperties);
  }
}
