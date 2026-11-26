
package com.ramanuj.docmind.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Standard API response wrapper containing operation status, message, response data and timestamp.")
public class ApiResponse<T> {

	@Schema(description = "Indicates whether the API operation completed successfully.", example = "true")
	private Boolean success;

	@Schema(description = "Human-readable response message.", example = "Request processed successfully")
	private String message;

	@Schema(description = "Response payload. Its schema is resolved from the endpoint's generic response type.")
	private T data;

	@Schema(description = "Date and time when the response was generated.", example = "2026-11-26T01:30:00")
	private LocalDateTime timestamp;
}
