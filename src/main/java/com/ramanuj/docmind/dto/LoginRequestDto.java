
package com.ramanuj.docmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credentials required to authenticate a user.")
public record LoginRequestDto(@Schema(description = "Registered username.", example = "string", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Username is required") String username, @Schema(description = "User account password.", example = "string", accessMode = 
		Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)@NotBlank(message = "Password is required") String password) {
	
}
