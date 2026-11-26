
package com.ramanuj.docmind.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentProcessingExceptionTest {

	@Test
	void constructorWithMessage_shouldPreserveMessage() {
		DocumentProcessingException exception = new DocumentProcessingException("Document parsing failed");

		assertThat(exception).isInstanceOf(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("Document parsing failed");
		assertThat(exception.getCause()).isNull();
	}

	@Test
	void noArgumentConstructor_shouldUseDefaultMessage() {
		DocumentProcessingException exception = new DocumentProcessingException();

		assertThat(exception.getMessage()).isEqualTo("Error in processing documents !!");
		assertThat(exception.getCause()).isNull();
	}

	@Test
	void constructorWithMessageAndCause_shouldPreserveMessageAndCause() {
		RuntimeException cause = new RuntimeException("File read failed");

		DocumentProcessingException exception = new DocumentProcessingException("Could not process document", cause);

		assertThat(exception).isInstanceOf(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("Could not process document");
		assertThat(exception.getCause()).isSameAs(cause);
	}
}
