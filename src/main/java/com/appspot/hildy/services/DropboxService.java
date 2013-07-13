package com.appspot.hildy.services;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.appspot.hildy.model.Credentials;
import com.appspot.hildy.model.HildyConfiguration;
import com.appspot.hildy.model.dropbox.DropboxAccount;
import com.appspot.hildy.model.dropbox.DropboxDelta;
import com.appspot.hildy.model.dropbox.DropboxFile;
import com.appspot.hildy.model.dropbox.DropboxFileMetadata;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class DropboxService {
	@Inject Logger logger;
	HttpTransport transport;
	Gson gson;
	Credentials credentials;
	
	@Inject
	public DropboxService(HttpTransport transport, Gson gson, @Assisted Credentials credentials) {
		this.transport = transport;
		this.gson = gson;
		this.credentials = credentials;
	}
	
	public DropboxAccount getAccount()
			throws IOException, DropboxServiceException {
		HttpRequestFactory requestFactory = transport.createRequestFactory(newOAuthParameters());
		HttpRequest req = requestFactory.buildGetRequest(new GenericUrl(HildyConfiguration.DROPBOX_ACCOUNT_URL));
		req.setThrowExceptionOnExecuteError(false);
		HttpResponse resp = req.execute();
		int actualErrorCode = resp.getStatusCode();
		String content = resp.parseAsString();
		resp.disconnect();
		if (actualErrorCode == 200) {
			DropboxAccount acc = gson.fromJson(content, DropboxAccount.class);
			return acc;
		} else {
			throw translateDropboxAPIError(actualErrorCode, content);
		}
	}
	
	public DropboxDelta getDelta(String cursor)
			throws IOException, DropboxServiceException {
		HttpRequestFactory requestFactory = transport.createRequestFactory(newOAuthParameters());
		DropboxDelta entriesDelta = new DropboxDelta();
		for (;;) {
			// TODO google oauth library cannot sign POST parameters, only GET
			GenericUrl url = new GenericUrl(HildyConfiguration.DROPBOX_DELTA_URL);
			url.set("cursor", cursor);
			HttpRequest req = requestFactory.buildPostRequest(url, null);
			req.setThrowExceptionOnExecuteError(false);
			HttpResponse resp = req.execute();
			int actualErrorCode = resp.getStatusCode();
			String content = resp.parseAsString();
			resp.disconnect();
			if (actualErrorCode == 304) {
				break;
			} else if (actualErrorCode == 200) {
				DropboxDelta chunkDelta = gson.fromJson(content, DropboxDelta.class);
				entriesDelta.entries.addAll(chunkDelta.entries);
				entriesDelta.cursor = chunkDelta.cursor;
				entriesDelta.reset = chunkDelta.reset;
				entriesDelta.has_more = chunkDelta.has_more;
				if (!entriesDelta.has_more) {
					break;
				}
			} else {
				throw translateDropboxAPIError(actualErrorCode, content);
			}
		}
		return entriesDelta;
	}
	
	public DropboxFile getFile(String path)
			throws IOException, DropboxServiceException {
		HttpRequestFactory requestFactory = transport.createRequestFactory(newOAuthParameters());
		HttpRequest req = requestFactory.buildGetRequest(new GenericUrl(HildyConfiguration.DROPBOX_PATH_URL + path));
		req.setThrowExceptionOnExecuteError(false);
		HttpResponse resp = req.execute();
		int actualErrorCode = resp.getStatusCode();
		String content = resp.parseAsString();
		resp.disconnect();
		if (actualErrorCode == 200) {
			DropboxFile file = new DropboxFile();
			file.contents = content;
			List<?> fileMetadata = (List<?>)resp.getHeaders().get("x-dropbox-metadata");
			if (fileMetadata != null && fileMetadata.size() > 0) {
				String md = (String)fileMetadata.get(0);
				if (md != null) {
					file.metadata = gson.fromJson(md, DropboxFileMetadata.class);
				}
			}
			return file;
		} else {
			throw translateDropboxAPIError(actualErrorCode, content);
		}
	}
	
	private OAuthParameters newOAuthParameters() {
		OAuthParameters parameters = new OAuthParameters();
		OAuthHmacSigner asigner = new OAuthHmacSigner();
		asigner.clientSharedSecret = HildyConfiguration.HILDY_APP_SECRET;
		asigner.tokenSharedSecret = credentials.tokenSecret;
		parameters.signer = asigner;
		parameters.consumerKey = HildyConfiguration.HILDY_APP_KEY;
		parameters.token = credentials.token;
		return parameters;
	}
	
	private DropboxServiceException translateDropboxAPIError(int httpErrorCode, String description) {
		DropboxServiceException ex;
		try {
			ex = gson.fromJson(description, DropboxServiceException.class);
		} catch (JsonSyntaxException e) {
			ex = new DropboxServiceException(description);
		}
		switch(httpErrorCode) {
		case 401:
			return ex.MustReauthenticate();
		case 403:
			return ex.BadOAuthRequest();
		case 503:
			return ex.TooManyRequests();
		case 507:
			return ex.QuotaExceeded();
		default:
			return ex;
		}
	}
}
