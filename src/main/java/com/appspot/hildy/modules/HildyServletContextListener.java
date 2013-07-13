package com.appspot.hildy.modules;

import com.appspot.hildy.servlets.BloggerServlet;
import com.appspot.hildy.servlets.EntriesServlet;
import com.appspot.hildy.servlets.FeedServlet;
import com.appspot.hildy.servlets.RegisterServlet;
import com.appspot.hildy.servlets.WelcomeServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class HildyServletContextListener extends GuiceServletContextListener {
	@Override
	protected Injector getInjector() {
		Injector inj = Guice.createInjector(new ServletModule() {
			@Override
			protected void configureServlets() {
				serve("/welcome").with(WelcomeServlet.class);
				serveRegex("/register/cb\\/?", "/register\\/?").with(RegisterServlet.class);
				serveRegex("/blogger/([0-9]+)/entry/([A-Za-z0-9_]+)\\/?$").with(EntriesServlet.class);
				serveRegex("/blogger/([0-9]+)\\/?$").with(BloggerServlet.class);
				serveRegex("/blogger/([0-9]+)/feed\\/?$").with(FeedServlet.class);
			}
		}, new AppEngineModule(), new DropboxModule(), new HildyModule());
		return inj;
	}
}
