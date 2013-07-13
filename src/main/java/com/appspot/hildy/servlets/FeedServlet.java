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

import com.appspot.hildy.model.Blog;
import com.appspot.hildy.model.Blogger;
import com.appspot.hildy.model.Entry;
import com.appspot.hildy.model.HildyConfiguration;
import com.appspot.hildy.model.MuDate;
import com.appspot.hildy.services.PersistenceService;
import com.appspot.hildy.services.TemplateService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("serial")
public class FeedServlet extends HttpServlet {
	@Inject Logger logger;
	@Inject PersistenceService persistenceService;
	@Inject TemplateService templateService;
	private Pattern urlPattern = Pattern.compile("/blogger/([0-9]+)/feed\\/?$");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("FeedServlet through " + req.getRequestURI());
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
	
		List<Entry> entries = persistenceService.getRecentEntries(blogger, 10);
		
		Blog blog = new Blog();
		blog.author = blogger.name;
		blog.title = blogger.name + " for Hildy";
		blog.host = HildyConfiguration.HILDY_HOST;
		blog.url = "/blogger/" + bloggerId;
		blog.feed = "/blogger/" + bloggerId + "/feed";
		if (entries.isEmpty()) {
			blog.updated = new MuDate();
		} else {
			blog.updated = entries.get(0).updated;
		}
		
		resp.setContentType("application/atom+xml");
		Map<String, Object> scopes = new HashMap<String, Object>();
		scopes.put("blog", blog);
		scopes.put("entries", entries);
		templateService.render("feed.mustache", resp.getWriter(), scopes);
	}
}
