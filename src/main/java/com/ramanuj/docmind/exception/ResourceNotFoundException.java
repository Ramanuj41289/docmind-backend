package com.ramanuj.docmind.exception;

public class ResourceNotFoundException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	public ResourceNotFoundException(String message) {
		super(message);
	}
	
	public ResourceNotFoundException() {
		super("Resource which you are looking for, is Not Found !!");
	}

	public ResourceNotFoundException(String message, Throwable throwable) {
		super(message, throwable);
	}

}