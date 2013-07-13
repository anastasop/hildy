package com.appspot.hildy.model;


public final class HildyConfiguration {
	// these are the application credential for oauth.
	// they are assigned by http://www.dropbox.com when the
	// app is registered
	public static final String HILDY_APP_KEY = "[key]";
	public static final String HILDY_APP_SECRET = "[secret]";
	
	public static final String DROPBOX_REQ_URI = "https://api.dropbox.com/1/oauth/request_token";
	public static final String DROPBOX_AUTH_URI = "https://www.dropbox.com/1/oauth/authorize";
	public static final String DROPBOX_TOKEN_URI = "https://api.dropbox.com/1/oauth/access_token";
	public static final String DROPBOX_ACCOUNT_URL = "https://api.dropbox.com/1/account/info";
	public static final String DROPBOX_PATH_URL = "https://api-content.dropbox.com/1/files/sandbox";
	public static final String DROPBOX_DELTA_URL = "https://api.dropbox.com/1/delta";
	public static final String HILDY_HOST = "hildy-blogger.appspot.com";
	public static final String HILDY_LOGIN_URL = "http://hildy-blogger.appspot.com/register";
	public static final String HILDY_CALLBACK_URL = "http://hildy-blogger.appspot.com/register/cb";
}
