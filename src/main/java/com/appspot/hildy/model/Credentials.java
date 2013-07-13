package com.appspot.hildy.model;

public class Credentials {
	public String token;
	public String tokenSecret;
	
	public Credentials(String token, String tokenSecret) {
		this.token = token;
		this.tokenSecret = tokenSecret;
	}
}
