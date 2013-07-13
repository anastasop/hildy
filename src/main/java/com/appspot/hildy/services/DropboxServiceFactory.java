package com.appspot.hildy.services;

import com.appspot.hildy.model.Credentials;

public interface DropboxServiceFactory {
	DropboxService create(Credentials cred);
}
