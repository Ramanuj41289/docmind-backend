
package com.ramanuj.docmind.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.config.AppProperties;
import com.ramanuj.docmind.dto.AdminStatusDto;
import com.ramanuj.docmind.dto.UpdateUserRoleRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
       return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(this::toUserDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        User user = findUser(userId);
        return toUserDto(user);
    }

    @Transactional
    public UserDto updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = findUser(userId);
        Role currentRole = user.getRole();
        Role newRole = request.role();

        if (isHitlApprovalRequired(currentRole, newRole)
                && !Boolean.TRUE.equals(request.approvalConfirmed())) {
            throw new ConflictException("Approval is required for high-risk role changes");
        }

        if (currentRole == newRole) {
            return toUserDto(user);
        }
        if (currentRole == Role.ADMIN && newRole == Role.USER) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ConflictException("Cannot demote the last administrator");
            }
        }
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        return toUserDto(updatedUser);
    }

    private boolean isHitlApprovalRequired(Role currentRole, Role newRole) {
        return appProperties.getHitl().isRequireAdminRoleUpdateApproval()
                && (currentRole == Role.ADMIN || newRole == Role.ADMIN)
                && currentRole != newRole;
    }

    @Transactional(readOnly = true)
    public AdminStatusDto getStats() {
        long totalUsers = userRepository.count();
        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        long totalRegularUsers = userRepository.countByRole(Role.USER);
        return new AdminStatusDto(totalUsers, totalAdmins, totalRegularUsers);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}