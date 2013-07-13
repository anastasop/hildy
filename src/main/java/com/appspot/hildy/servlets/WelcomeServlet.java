package com.appspot.hildy.servlets;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.hildy.model.Blogger;
import com.appspot.hildy.services.PersistenceService;
import com.appspot.hildy.services.TemplateService;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("serial")
public class WelcomeServlet extends HttpServlet {
	@Inject public Logger logger;
	@Inject public PersistenceService persistenceService;
	@Inject public TemplateService templateService;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("Fetching all bloggers from datastore");
		List<Blogger> bloggers = Collections.emptyList();
		try {
			bloggers = persistenceService.getAllBloggers();
		} catch (DatastoreFailureException e) {
			logger.info(String.format("DatastoreFailureException: '%s'", e.getMessage()));
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("text/plain");
			resp.getWriter().printf("Internal Error");
			return;
		}
		templateService.render("welcome.mustache", resp.getWriter(), bloggers);
	}
}
