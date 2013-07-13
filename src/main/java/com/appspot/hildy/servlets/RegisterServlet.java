package com.appspot.hildy.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.hildy.model.Blogger;
import com.appspot.hildy.model.Credentials;
import com.appspot.hildy.model.HildyConfiguration;
import com.appspot.hildy.model.MuDate;
import com.appspot.hildy.model.dropbox.DropboxAccount;
import com.appspot.hildy.services.DropboxService;
import com.appspot.hildy.services.DropboxServiceException;
import com.appspot.hildy.services.DropboxServiceFactory;
import com.appspot.hildy.services.PersistenceService;
import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.http.HttpTransport;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("serial")
public class RegisterServlet extends HttpServlet {
	@Inject Logger logger;
	@Inject HttpTransport transport;
	@Inject MemcacheService memcache;
	@Inject PersistenceService persistenceService;
	@Inject DropboxServiceFactory dropboxServiceFactory;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String url = req.getRequestURI();
		if (url.endsWith("cb") || url.endsWith("cb/")) {
			doOAuthCallback(req, resp);
		} else if (url.endsWith("register") || url.endsWith("register/")) {
			doOAuthRegistration(req, resp);
		} else {
			resp.sendRedirect("/welcome");
			return;
		}
	}
	
	private void doOAuthRegistration(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		logger.info("asking temporary oauth token from dropbox");
		OAuthCredentialsResponse requestTokenResponse;
		try {
			OAuthGetTemporaryToken requestToken =
					new OAuthGetTemporaryToken(HildyConfiguration.DROPBOX_REQ_URI);
			OAuthHmacSigner signer = new OAuthHmacSigner();
			signer.clientSharedSecret = HildyConfiguration.HILDY_APP_SECRET;
			requestToken.signer = signer;
			requestToken.consumerKey = HildyConfiguration.HILDY_APP_KEY;
			requestToken.callback = HildyConfiguration.HILDY_CALLBACK_URL;
			requestToken.transport = transport;
			requestTokenResponse = requestToken.execute();
			signer.tokenSharedSecret = requestTokenResponse.tokenSecret;
			logger.info(String.format("Got temporary token. Key '%s' Secret '%s'",
					requestTokenResponse.token, requestTokenResponse.tokenSecret));
		} catch (IOException e) {
			logger.info(String.format("IOException while calling dropbox: '%s'", e.getMessage()));
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		logger.info("writing to memcache");
		try {
			boolean writtenToMemcached = memcache.put(
					requestTokenResponse.token, requestTokenResponse.tokenSecret,
					Expiration.byDeltaSeconds(120), SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
			if (!writtenToMemcached) {
				logger.warning("doOAuthRegistration: token not written to memcache because is already present. Weird!! Replacing it.");
				writtenToMemcached = memcache.put(requestTokenResponse.token, requestTokenResponse.tokenSecret,
						Expiration.byDeltaSeconds(30), SetPolicy.SET_ALWAYS);
				if (!writtenToMemcached) {
					logger.warning("doOAuthRegistration: token not written to memcache although policy was SET_ALWAYS. Aborting");
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		} catch(MemcacheServiceException e) {
			logger.info(String.format("MemcacheServiceException: '%s'", e.getMessage()));
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		OAuthAuthorizeTemporaryTokenUrl authorizeUrl =
				new OAuthAuthorizeTemporaryTokenUrl(HildyConfiguration.DROPBOX_AUTH_URI);
		authorizeUrl.set("oauth_callback", HildyConfiguration.HILDY_CALLBACK_URL);
		authorizeUrl.set("oauth_token", requestTokenResponse.token);
		String redirectUrl = authorizeUrl.build();
		logger.info(String.format("Redirecting to '%s'", redirectUrl));
		resp.sendRedirect(redirectUrl);
	}
	
	private void doOAuthCallback(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String oauth_token = req.getParameter("oauth_token");
		String oauth_verifier = req.getParameter("oauth_verifier");
		String not_approved = req.getParameter("not_approved");
		
		if ("true".equals(not_approved)) {
			resp.sendRedirect("/notapproved.html");
			return;
		}
		String tokenSharedSecret = "";
		try {
			if (oauth_token != null) {
				tokenSharedSecret = (String)memcache.get(oauth_token);
			}
			if (oauth_token == null || tokenSharedSecret == null) {
				logger.info("OAuth unknown token in request");
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} catch(MemcacheServiceException e) {
			logger.info(String.format("MemcacheServiceException: '%s'", e.getMessage()));
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		OAuthCredentialsResponse accessTokenResponse;
		try {
			OAuthGetAccessToken accessToken =
					new OAuthGetAccessToken(HildyConfiguration.DROPBOX_TOKEN_URI);
			accessToken.consumerKey = HildyConfiguration.HILDY_APP_KEY;
			OAuthHmacSigner signer = new OAuthHmacSigner();
			signer.clientSharedSecret = HildyConfiguration.HILDY_APP_SECRET;
			signer.tokenSharedSecret = tokenSharedSecret;
			accessToken.signer = signer;
			accessToken.transport = transport;
			accessToken.temporaryToken = oauth_token;
			accessToken.verifier = oauth_verifier;
			accessTokenResponse = accessToken.execute();
			logger.info(String.format("Got access token. Key '%s' Secret '%s'",
					accessTokenResponse.token, accessTokenResponse.tokenSecret));
		} catch (IOException e) {
			logger.info(String.format("IOException while calling dropbox: '%s'", e.getMessage()));
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		try {
			DropboxService srv = dropboxServiceFactory.create(
					new Credentials(accessTokenResponse.token, accessTokenResponse.tokenSecret));
			DropboxAccount acc = srv.getAccount();
			Blogger blogger = new Blogger();
			blogger.country = acc.country;
			blogger.dropboxUID = acc.uid;
			blogger.name = acc.display_name;

			blogger.token = accessTokenResponse.token;
			blogger.tokenSecret = accessTokenResponse.tokenSecret;
			blogger.registeredAt = new MuDate();
			persistenceService.putBlogger(blogger);
			resp.sendRedirect("/welcome");
		} catch (DropboxServiceException e) {
			logger.info(String.format("Dropbox API error: '%s'", e.getMessage()));
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (DatastoreFailureException e) {
			logger.info(String.format("Datastore API error: '%s'", e.getMessage()));
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
}
