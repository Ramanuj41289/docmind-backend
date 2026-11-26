

package com.ramanuj.docmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterUserRequest", description = "Request payload for creating a new user account.")
public record RegisterUserRequest(@Schema(description = "Username for the new account. Must contain between 3 and 50 characters.", 
	example = "ramanuj41289", requiredMode = Schema.RequiredMode.REQUIRED)@NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
        @Schema(description = "Valid email address for the new account.", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format")  @Size(max = 255, message = "Email is too long")
        String email, @Schema(description = "Account password. Must contain between 8 and 72 characters.", accessMode = Schema.AccessMode.WRITE_ONLY, 
        requiredMode = Schema.RequiredMode.REQUIRED)@NotBlank(message = "Password is required")@Size(min = 8, max = 72,
        message = "Password must be between 8 and 72 characters") String password) {
	
}

