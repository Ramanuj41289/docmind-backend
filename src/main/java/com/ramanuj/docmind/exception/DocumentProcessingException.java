package com.ramanuj.docmind.exception;

public class DocumentProcessingException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	public DocumentProcessingException(String message) {
		super(message);
	}
	
	public DocumentProcessingException() {
		super("Error in processing documents !!");
	}

	public DocumentProcessingException(String message, Throwable throwable) {
		super(message, throwable);
	}

}