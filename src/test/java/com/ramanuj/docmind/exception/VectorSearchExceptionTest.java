
package com.ramanuj.docmind.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VectorSearchExceptionTest {

	@Test
	void constructor_shouldPreserveMessageAndCause() {

		RuntimeException cause = new RuntimeException("Vector database unavailable");

		VectorSearchException exception = new VectorSearchException("Vector search failed", cause);

		assertThat(exception).isInstanceOf(RuntimeException.class);

		assertThat(exception.getMessage()).isEqualTo("Vector search failed");

		assertThat(exception.getCause()).isSameAs(cause);
	}
}
