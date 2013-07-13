package com.appspot.hildy.modules;

import java.io.File;

import com.appspot.hildy.model.dropbox.DropboxDate;
import com.appspot.hildy.model.dropbox.DropboxDeltaEntry;
import com.appspot.hildy.services.PersistenceService;
import com.appspot.hildy.services.SyncServiceFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.petebevin.markdown.MarkdownProcessor;

public class HildyModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(Gson.class).toInstance(new GsonBuilder()
			.registerTypeAdapter(DropboxDate.class, new DropboxDate.DropboxDateTypeAdapter())
			.registerTypeAdapter(DropboxDeltaEntry.class, new DropboxDeltaEntry.DropboxDeltaEntryDeserializer())
			.registerTypeAdapter(DropboxDeltaEntry.class, new DropboxDeltaEntry.DropboxDeltaEntrySerializer())
			.serializeNulls()
			.setPrettyPrinting()
			.create());
		bind(PersistenceService.class).in(Singleton.class);
		bind(MarkdownProcessor.class);
		bind(MustacheFactory.class).toInstance(new DefaultMustacheFactory(new File("./WEB-INF/templates/")));
		install(new FactoryModuleBuilder().build(SyncServiceFactory.class));
	}
}
