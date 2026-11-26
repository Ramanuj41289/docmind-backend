
package com.ramanuj.docmind.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ramanuj.docmind.dto.ApiResponse;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler exceptionHandler;

	@BeforeEach
	void setUp() {
		exceptionHandler = new GlobalExceptionHandler();
	}

	@Test
	void handleNotFound_shouldReturnNotFoundResponse() {

		ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(exception);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());
		assertEquals("User not found", response.getBody().getMessage());
		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleProcessingError_shouldReturnUnprocessableContent() {

		DocumentProcessingException exception = new DocumentProcessingException("PDF parsing failed");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleProcessingError(exception);

		assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());

		assertNotNull(response.getBody());
		assertFalse(response.getBody().getSuccess());

		assertEquals("Failed to process document", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleMaxSize_shouldReturnContentTooLarge() {

		MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(25 * 1024 * 1024L);

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMaxSize(exception);

		assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());

		assertNotNull(response.getBody());
		assertFalse(response.getBody().getSuccess());

		assertEquals("File size exceeds the allowed limit of 25MB", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleValidation_shouldReturnFieldErrors() throws Exception {

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

		bindingResult.addError(new FieldError("request", "username", "Username is required"));

		bindingResult.addError(new FieldError("request", "email", "Email is invalid"));

		Method method = GlobalExceptionHandler.class.getDeclaredMethod("handleValidation",
				MethodArgumentNotValidException.class);

		MethodParameter methodParameter = new MethodParameter(method, 0);

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

		ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidation(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());
		assertEquals("Validation failed", response.getBody().getMessage());

		Map<String, String> errors = response.getBody().getData();

		assertNotNull(errors);
		assertEquals(2, errors.size());
		assertEquals("Username is required", errors.get("username"));
		assertEquals("Email is invalid", errors.get("email"));

		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleValidation_shouldKeepFirstErrorForDuplicateField() throws Exception {

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

		bindingResult.addError(new FieldError("request", "username", "Username is required"));

		bindingResult.addError(new FieldError("request", "username", "Username must contain at least 3 characters"));

		Method method = GlobalExceptionHandler.class.getDeclaredMethod("handleValidation",
				MethodArgumentNotValidException.class);

		MethodParameter methodParameter = new MethodParameter(method, 0);

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

		ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidation(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		assertEquals(1, response.getBody().getData().size());

		assertEquals("Username is required", response.getBody().getData().get("username"));
	}

	@Test
	void handleAccessDenied_shouldReturnForbiddenResponse() {

		AccessDeniedException exception = new AccessDeniedException("Access denied");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDenied(exception);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());

		assertEquals("You do not have permission to access this resource", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleAuthentication_shouldReturnUnauthorizedResponse() {

		AuthenticationException exception = new AuthenticationException("Authentication failed") {
			private static final long serialVersionUID = 1L;
		};

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthentication(exception);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());

		assertEquals("Invalid username or password", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleDataIntegrity_shouldReturnConflictResponse() {

		DataIntegrityViolationException exception = new DataIntegrityViolationException("Duplicate username");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDataIntegrity(exception);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());

		assertEquals("A record with the same unique value already exists", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleIllegalArgument_shouldReturnBadRequestResponse() {

		IllegalArgumentException exception = new IllegalArgumentException("Invalid document ID");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgument(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());

		assertEquals("Invalid document ID", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleGeneric_shouldReturnInternalServerError() {

		Exception exception = new Exception("Unexpected database failure");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeneric(exception);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

		assertNotNull(response.getBody());
		assertFalse(response.getBody().getSuccess());

		assertEquals("An unexpected error occurred", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleConflictException_shouldReturnConflictResponse() {

		ConflictException exception = new ConflictException("Conversation already exists");

		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleConflictException(exception);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());

		assertFalse(response.getBody().getSuccess());

		assertEquals("Conversation already exists", response.getBody().getMessage());

		assertNull(response.getBody().getData());
		assertNotNull(response.getBody().getTimestamp());
	}
}
