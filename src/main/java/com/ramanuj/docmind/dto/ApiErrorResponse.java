
package com.ramanuj.docmind.dto;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standard error response returned when an API request fails.")
public record ApiErrorResponse(

		@Schema(description = "Indicates whether the operation succeeded.", example = "false") Boolean success,

		@Schema(description = "Description of the error.", example = "Validation failed") String message,

		@Schema(description = "Additional error details. Null for standard errors and a field-to-message map for validation errors.", nullable = true) Map<String, String> data,

		@Schema(description = "Date and time when the response was generated.", example = "2026-11-26T16:30:00") LocalDateTime timestamp) {

}
