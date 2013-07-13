package com.appspot.hildy.services;

@SuppressWarnings("serial")
public class DropboxServiceException extends Exception {
	public String error;
	
	public DropboxServiceException() {
		this.error = "";
	}
	
	public DropboxServiceException(String message) {
		this.error = message;
	}

	public boolean mustReauthenticate;
	public boolean badOAuthRequest;
	public boolean quotaExceeded;
	public boolean tooManyRequests;
	
	public DropboxServiceException MustReauthenticate() {
		this.mustReauthenticate = true;
		return this;
	}
	
	public DropboxServiceException BadOAuthRequest() {
		this.badOAuthRequest = true;
		return this;
	}
	
	public DropboxServiceException QuotaExceeded() {
		this.quotaExceeded = true;
		return this;
	}
	
	public DropboxServiceException TooManyRequests() {
		this.tooManyRequests = true;
		return this;
	}
	
	@Override
	public String getMessage() {
		return error;
	}
}
