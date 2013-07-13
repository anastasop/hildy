package com.appspot.hildy.modules;

import com.appspot.hildy.services.DropboxServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class DropboxModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().build(DropboxServiceFactory.class));
	}
}
