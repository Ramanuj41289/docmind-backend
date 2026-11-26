
package com.ramanuj.docmind.dto;

import com.ramanuj.docmind.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateUserRoleRequest", description = "Request payload for updating a user's assigned role.")
public record UpdateUserRoleRequest(@Schema(description = "Role to assign to the user.", example = "ADMIN", 
		requiredMode = Schema.RequiredMode.REQUIRED)@NotNull(message = "Role is required") Role role) {
	
}
