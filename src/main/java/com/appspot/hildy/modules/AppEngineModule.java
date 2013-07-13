package com.appspot.hildy.modules;

import static com.google.appengine.api.datastore.DatastoreServiceConfig.Builder.withReadPolicy;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.ImplicitTransactionManagementPolicy;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.AbstractModule;

public class AppEngineModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(MemcacheService.class).toInstance(MemcacheServiceFactory.getMemcacheService());
		bind(DatastoreService.class).toInstance(DatastoreServiceFactory.getDatastoreService(
				withReadPolicy(new ReadPolicy(Consistency.EVENTUAL))
				.implicitTransactionManagementPolicy(ImplicitTransactionManagementPolicy.NONE)));
		bind(HttpTransport.class).to(UrlFetchTransport.class);
	}
}
