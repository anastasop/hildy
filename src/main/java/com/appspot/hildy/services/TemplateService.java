package com.appspot.hildy.services;

import java.io.Writer;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.Inject;

public class TemplateService {
	private MustacheFactory mustacheFactory;
	
	@Inject
	public TemplateService(MustacheFactory mustacheFactory) {
		this.mustacheFactory = mustacheFactory;
	}
		
	public void render(String name, Writer w, Object o) {
		Mustache m = mustacheFactory.compile(name);
		m.execute(w, o);
	}
}
