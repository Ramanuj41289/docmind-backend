
package com.ramanuj.docmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Authentication response containing the JWT access token and authenticated user details.")
public record LoginResponseDto(
		@Schema(description = "JWT access token used to authenticate subsequent API requests.", example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
		@Schema(description = "Details of the authenticated user.") UserDto user) {

}
