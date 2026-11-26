
package com.ramanuj.docmind.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceNotFoundExceptionTest {

	@Test
	void constructorWithMessage_shouldPreserveMessage() {
		ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

		assertThat(exception).isInstanceOf(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("User not found");
		assertThat(exception.getCause()).isNull();
	}

	@Test
	void noArgumentConstructor_shouldUseDefaultMessage() {
		ResourceNotFoundException exception = new ResourceNotFoundException();

		assertThat(exception.getMessage()).isEqualTo("Resource which you are looking for, is Not Found !!");
		assertThat(exception.getCause()).isNull();
	}

	@Test
	void constructorWithMessageAndCause_shouldPreserveMessageAndCause() {
		RuntimeException cause = new RuntimeException("Database connection failed");

		ResourceNotFoundException exception = new ResourceNotFoundException("Resource lookup failed", cause);

		assertThat(exception).isInstanceOf(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("Resource lookup failed");
		assertThat(exception.getCause()).isSameAs(cause);
	}
}
