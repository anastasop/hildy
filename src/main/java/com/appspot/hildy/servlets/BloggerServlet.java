package com.appspot.hildy.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.hildy.model.Blogger;
import com.appspot.hildy.model.Entry;
import com.appspot.hildy.services.PersistenceService;
import com.appspot.hildy.services.SyncService;
import com.appspot.hildy.services.TemplateService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("serial")
public class BloggerServlet extends HttpServlet {
	@Inject Logger logger;
	@Inject SyncService syncService;
	@Inject PersistenceService persistenceService;
	@Inject TemplateService templateService;
	private Pattern urlPattern = Pattern.compile("/blogger/([0-9]+)\\/?$");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("BloggerServlet through " + req.getRequestURI());
		Matcher m = urlPattern.matcher(req.getRequestURI());
		if (!m.matches()) {
			throw new RuntimeException("url " + req.getRequestURI() + " does not match " + urlPattern.pattern());
		}
		Long bloggerId = Long.valueOf(m.group(1));
		
		Blogger blogger = persistenceService.getBlogger(bloggerId);
		if (blogger == null) {
			resp.sendRedirect("/welcome");
			return;
		}
		
		SyncService.SyncStatus status = syncService.sync(bloggerId);
		switch (status) {
		case OK:
			break;
		case FAILED:
			// something failed but don't worry.
			// Show the current posts and hope
			// everything goes well for the next sync
			// Hildy is an eventually consistent application
			break;
		case UNAUTHORISED:
			// this is serious. The blogger has uninstalled hildy
			// TODO remove posts from datastore
			resp.setStatus(HttpServletResponse.SC_GONE);
			templateService.render("unauthorised.mustache", resp.getWriter(), blogger);
			return;
		}
		
		List<Entry> entries = persistenceService.getRecentEntries(blogger, 0);
		
		Map<String, Object> scopes = new HashMap<String, Object>();
		scopes.put("blogger", blogger);
		scopes.put("entries", entries);
		templateService.render("blogger.mustache", resp.getWriter(), scopes);
	}
}
