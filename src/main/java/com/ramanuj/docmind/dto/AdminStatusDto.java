
package com.ramanuj.docmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminStatus", description = "Summary of registered users grouped by account role.")
public record AdminStatusDto(@Schema(description = "Total number of registered users.", example = "150") long totalUsers,
		@Schema(description = "Total number of administrator accounts.", example = "5") long totalAdmins,
		@Schema(description = "Total number of regular user accounts.", example = "145") long totalRegularUsers) {
	
}
