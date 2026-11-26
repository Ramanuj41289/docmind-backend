
package com.ramanuj.docmind.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.ramanuj.docmind.dto.AdminStatusDto;
import com.ramanuj.docmind.dto.UpdateUserRoleRequest;
import com.ramanuj.docmind.dto.UserDto;
import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.exception.ConflictException;
import com.ramanuj.docmind.exception.ResourceNotFoundException;
import com.ramanuj.docmind.repository.UserRepository;

@DataJpaTest
@Import(AdminService.class)
class AdminServiceIntegrationTest {

	@Autowired
	private AdminService adminService;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	void getAllUsersShouldReturnUsersInAscendingIdOrder() {

		User first = createUser("adminuser1", "admin1@example.com", Role.ADMIN);
		User second = createUser("regularuser1", "user1@example.com", Role.USER);
		User third = createUser("regularuser2", "user2@example.com", Role.USER);

		List<UserDto> result = adminService.getAllUsers();

		assertThat(result).hasSize(3);

		assertThat(result.get(0).id()).isEqualTo(first.getId());
		assertThat(result.get(1).id()).isEqualTo(second.getId());
		assertThat(result.get(2).id()).isEqualTo(third.getId());
	}

	@Test
	void getAllUsersShouldReturnEmptyListWhenNoUsersExist() {

		List<UserDto> result = adminService.getAllUsers();

		assertThat(result).isEmpty();
	}

	@Test
	void getUserByIdShouldReturnUserDetails() {

		User user = createUser("adminuser2", "admin2@example.com", Role.ADMIN);

		UserDto result = adminService.getUserById(user.getId());

		assertThat(result.id()).isEqualTo(user.getId());
		assertThat(result.username()).isEqualTo("adminuser2");
		assertThat(result.email()).isEqualTo("admin2@example.com");
		assertThat(result.role()).isEqualTo(Role.ADMIN);
	}

	@Test
	void getUserByIdShouldThrowWhenUserDoesNotExist() {

		assertThatThrownBy(() -> adminService.getUserById(99999L)).isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found with ID: 99999");
	}

	@Test
	void updateUserRoleShouldPromoteRegularUserToAdmin() {

		User user = createUser("promoteuser", "promote@example.com", Role.USER);

		UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.ADMIN);

		UserDto result = adminService.updateUserRole(user.getId(), request);

		assertThat(result.id()).isEqualTo(user.getId());
		assertThat(result.role()).isEqualTo(Role.ADMIN);

		entityManager.flush();
		entityManager.clear();

		User updatedUser = userRepository.findById(user.getId()).orElseThrow();

		assertThat(updatedUser.getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void updateUserRoleShouldDemoteAdminWhenAnotherAdminExists() {

		User admin1 = createUser("demoteadmin1", "demote1@example.com", Role.ADMIN);

		createUser("demoteadmin2", "demote2@example.com", Role.ADMIN);

		UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.USER);

		UserDto result = adminService.updateUserRole(admin1.getId(), request);

		assertThat(result.id()).isEqualTo(admin1.getId());
		assertThat(result.role()).isEqualTo(Role.USER);

		entityManager.flush();
		entityManager.clear();

		User updatedUser = userRepository.findById(admin1.getId()).orElseThrow();

		assertThat(updatedUser.getRole()).isEqualTo(Role.USER);
		assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
	}

	@Test
	void updateUserRoleShouldRejectDemotionOfLastAdministrator() {

		User admin = createUser("lastadmin", "lastadmin@example.com", Role.ADMIN);

		UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.USER);

		assertThatThrownBy(() -> adminService.updateUserRole(admin.getId(), request))
				.isInstanceOf(ConflictException.class).hasMessage("Cannot demote the last administrator");

		entityManager.flush();
		entityManager.clear();

		User unchangedAdmin = userRepository.findById(admin.getId()).orElseThrow();

		assertThat(unchangedAdmin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
	}

	@Test
	void updateUserRoleShouldNotChangeUserWhenRequestedRoleIsSame() {

		User user = createUser("sameRoleUser", "samerole@example.com", Role.USER);

		UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.USER);

		UserDto result = adminService.updateUserRole(user.getId(), request);

		assertThat(result.id()).isEqualTo(user.getId());
		assertThat(result.role()).isEqualTo(Role.USER);
		User persistedUser = userRepository.findById(user.getId()).orElseThrow();
		assertThat(persistedUser.getRole()).isEqualTo(Role.USER);
	}

	@Test
	void getStatsShouldReturnCorrectUserCounts() {

		createUser("statsadmin1", "statsadmin1@example.com", Role.ADMIN);
		createUser("statsadmin2", "statsadmin2@example.com", Role.ADMIN);
		createUser("statsuser1", "statsuser1@example.com", Role.USER);
		createUser("statsuser2", "statsuser2@example.com", Role.USER);
		createUser("statsuser3", "statsuser3@example.com", Role.USER);

		AdminStatusDto result = adminService.getStats();

		assertThat(result.totalUsers()).isEqualTo(5);
		assertThat(result.totalAdmins()).isEqualTo(2);
		assertThat(result.totalRegularUsers()).isEqualTo(3);
	}

	private User createUser(String username, String email, Role role) {

		User user = User.builder().username(username).email(email).password("encoded-password").role(role).build();

		return entityManager.persistAndFlush(user);
	}
}
