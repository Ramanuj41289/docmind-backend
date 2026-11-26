
package com.ramanuj.docmind.dto;

import com.ramanuj.docmind.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "User", description = "User account information returned by authentication and administration APIs.")
public record UserDto(@Schema(description = "Unique user identifier.", example = "101") Long id, @Schema(description = "Registered username.", 
	example = "ramanuj41289") String username, @Schema(description = "Registered email address.", example = "user@example.com")
		String email, @Schema(description = "Assigned user role.", example = "USER") Role role) {
	
}
