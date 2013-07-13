package com.appspot.hildy.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.appspot.hildy.model.Blogger;
import com.appspot.hildy.model.Entry;
import com.appspot.hildy.model.MuDate;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyFactory.Builder;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.inject.Inject;


public class PersistenceService {
	@Inject public Logger logger;
	public DatastoreService datastore;
	
	@Inject
	public PersistenceService(DatastoreService datastore) {
		this.datastore = datastore;
	}
	
	public long putBlogger(Blogger blogger)
			throws DatastoreFailureException {
		Entity e;
		if (blogger.bloggerId != null) {
			e = new Entity("Blogger", blogger.bloggerId);
		} else {
			e = new Entity("Blogger");
		}
		e.setProperty("name", blogger.name);
		e.setProperty("country", blogger.country);
		e.setUnindexedProperty("aboutMe", blogger.aboutMe);
		e.setProperty("registeredAt", blogger.registeredAt.getDate());
		e.setUnindexedProperty("token", blogger.token);
		e.setUnindexedProperty("tokenSecret", blogger.tokenSecret);
		e.setUnindexedProperty("dropboxUID", blogger.dropboxUID);
		e.setUnindexedProperty("entriesCursor", blogger.entriesCursor);
		Key key = datastore.put(e);
		return key.getId();
	}
	
	public Blogger getBlogger(Long id)
			throws DatastoreFailureException {
		try {
			Entity e = datastore.get(KeyFactory.createKey("Blogger", id));
			Blogger blogger = new Blogger();
			blogger.bloggerId = e.getKey().getId();
			blogger.name = (String)e.getProperty("name");
			blogger.country = (String)e.getProperty("country");
			blogger.aboutMe = (String)e.getProperty("aboutMe");
			blogger.registeredAt = new MuDate((Date)e.getProperty("registeredAt"));
			blogger.token = (String)e.getProperty("token");
			blogger.tokenSecret = (String)e.getProperty("tokenSecret");
			blogger.dropboxUID = (Long)e.getProperty("dropboxUID");
			blogger.entriesCursor = (String)e.getProperty("entriesCursor");
			return blogger;
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	public List<Blogger> getAllBloggers()
			throws DatastoreFailureException {
		Query q = new Query("Blogger");
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> entities = pq.asList(FetchOptions.Builder.withDefaults());
		if (entities == null) {
			return Collections.emptyList();
		}
		List<Blogger> bloggers = new ArrayList<Blogger>();
		for (Entity e: entities) {
			Blogger blogger = new Blogger();
			blogger.bloggerId = e.getKey().getId();
			blogger.name = (String)e.getProperty("name");
			blogger.country = (String)e.getProperty("country");
			blogger.aboutMe = (String)e.getProperty("aboutMe");
			blogger.registeredAt = new MuDate((Date)e.getProperty("registeredAt"));
			blogger.token = (String)e.getProperty("token");
			blogger.tokenSecret = (String)e.getProperty("tokenSecret");
			blogger.dropboxUID = (Long)e.getProperty("dropboxUID");
			blogger.entriesCursor = (String)e.getProperty("entriesCursor");
			bloggers.add(blogger);
		}
		return bloggers;
	}
	
	public Entry getEntry(long bloggerId, String slug)
			throws DatastoreFailureException {
		try {
			Key key = new Builder(KeyFactory.createKey("Blogger", bloggerId))
				.addChild("Entry", slug).getKey();
			Entity e = datastore.get(key);
			Entry entry = new Entry();
			entry.title = (String)e.getProperty("title");
			entry.slug = (String)e.getProperty("slug");
			entry.contentMarkdown = (Text)e.getProperty("contentMarkdown");
			entry.contentHtml = (Text)e.getProperty("contentHtml");
			entry.contentHtmlValue = entry.contentHtml.getValue();
			entry.published = new MuDate((Date)e.getProperty("published"));
			entry.updated = new MuDate((Date)e.getProperty("updated"));
			entry.entryKey = e.getKey().getName();
			return entry;
		} catch (EntityNotFoundException e1) {
			return null;
		}
	}
	
	public List<Entry> getRecentEntries(Blogger blogger, int count) {
		Query q = new Query("Entry", KeyFactory.createKey("Blogger", blogger.bloggerId));
//		q.addSort("updated", SortDirection.DESCENDING);
		Iterable<Entity> entities;
		if (count > 0) {
			entities = datastore.prepare(q).asIterable(FetchOptions.Builder.withLimit(count).prefetchSize(count));
		} else {
			entities = datastore.prepare(q).asIterable();
		}
		List<Entry> entries = new ArrayList<Entry>();
		for (Entity e: entities) {
			Entry entry = new Entry();
			entry.title = (String)e.getProperty("title");
			entry.slug = (String)e.getProperty("slug");
			entry.contentMarkdown = (Text)e.getProperty("contentMarkdown");
			entry.contentHtml = (Text)e.getProperty("contentHtml");
			entry.contentHtmlValue = entry.contentHtml.getValue();
			entry.published = new MuDate((Date)e.getProperty("published"));
			entry.updated = new MuDate((Date)e.getProperty("updated"));
			entry.entryKey = e.getKey().getName();
			entries.add(entry);
		}
		return entries;
	}
}
