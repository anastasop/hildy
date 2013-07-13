package com.appspot.hildy.servlets;

import java.io.IOException;
import java.util.HashMap;
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
import com.appspot.hildy.services.TemplateService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("serial")
public class EntriesServlet extends HttpServlet {
	@Inject Logger logger;
	@Inject PersistenceService persistenceService;
	@Inject TemplateService templateService;
	private Pattern urlPattern = Pattern.compile("/blogger/([0-9]+)/entry/([A-Za-z0-9_]+)\\/?$");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("EntriesServlet through " + req.getRequestURI());
		Matcher m = urlPattern.matcher(req.getRequestURI());
		if (!m.matches()) {
			throw new RuntimeException("url " + req.getRequestURI() + " does not match " + urlPattern.pattern());
		}
		Long bloggerId = Long.valueOf(m.group(1));
		String slug = m.group(2);
		
		Blogger blogger = persistenceService.getBlogger(bloggerId);
		Entry entry = persistenceService.getEntry(bloggerId, slug);
		
		Map<String, Object> scopes = new HashMap<String, Object>();
		scopes.put("blogger", blogger);
		scopes.put("entry", entry);
		templateService.render("entry.mustache", resp.getWriter(), scopes);
	}
}
