package com.appspot.hildy.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.appspot.hildy.model.Credentials;
import com.appspot.hildy.model.dropbox.DropboxDelta;
import com.appspot.hildy.model.dropbox.DropboxDeltaEntry;
import com.appspot.hildy.model.dropbox.DropboxFile;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyFactory.Builder;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.petebevin.markdown.MarkdownProcessor;

public class SyncService {
	public enum SyncStatus {
		OK,
		FAILED,
		UNAUTHORISED
	}
	
	@Inject Logger logger;
	DropboxServiceFactory dropboxServiceFactory;
	DatastoreService datastoreService;
	MarkdownProcessor markdownProcessor;
	
	@Inject
	public SyncService(
			DropboxServiceFactory dropboxServiceFactory,
			DatastoreService datastoreService,
			MarkdownProcessor markdownProcessor) {
		this.dropboxServiceFactory = dropboxServiceFactory;
		this.datastoreService = datastoreService;
		this.markdownProcessor = markdownProcessor;
	}
	
	private String slugFromFilePathName(String path) {
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if (path.endsWith(".md") || path.endsWith(".markdown") || path.endsWith(".txt")) {
			path = path.substring(0, path.lastIndexOf('.'));
		}
		path = path.replaceAll("[^A-Za-z0-9\\_\\-]+", " ").toLowerCase().trim();
		String slug = Joiner.on('-').skipNulls().join(path.split("[ \t]+"));
		return slug;
	}
	
	public SyncStatus sync(long bloggerId) {
		List<Entity> entitiesToInsert = new ArrayList<Entity>();
		List<Key> keysToDelete = new ArrayList<Key>();
		boolean rePutBlogger = false;
		Entity blogger;
		
		try {
			try {
				blogger = datastoreService.get(KeyFactory.createKey("Blogger", bloggerId));
			} catch (EntityNotFoundException e) {
				logger.info("there isn't a blogger with id: " + bloggerId);
				return SyncStatus.FAILED;
			} catch (DatastoreFailureException e) {
				logger.info("DatastoreFailureException: " + e.getMessage());
				return SyncStatus.FAILED;
			}
			
			logger.info(String.format("synchronizing entries for blogger '%s' with id %d",
					blogger.getProperty("name"), bloggerId));
			
			String token = (String)blogger.getProperty("token");
			String tokenSecret = (String)blogger.getProperty("tokenSecret");
			String entriesCursor = (String)blogger.getProperty("entriesCursor");
			
			DropboxService dropbox = dropboxServiceFactory.create(
					new Credentials(token, tokenSecret));
			DropboxDelta delta = dropbox.getDelta(entriesCursor);
			
			for (DropboxDeltaEntry deltaEntry: delta.entries) {
				if (deltaEntry.path.startsWith("draft-")) {
					continue;
				}
				String slug = slugFromFilePathName(deltaEntry.path);
				if ("aboutme".equals(slug)) {
					if (deltaEntry.metadata == null || deltaEntry.metadata.is_deleted) {
						blogger.setProperty("aboutMe", null);
					} else {
						DropboxFile file = dropbox.getFile(deltaEntry.path);
						blogger.setProperty("aboutMe", markdownProcessor.markdown(file.contents));
					}
					rePutBlogger = true;
				} else {
					String entryKey = slug;
					if (deltaEntry.metadata == null || deltaEntry.metadata.is_deleted) {
						keysToDelete.add(new Builder(
								KeyFactory.createKey("Blogger", bloggerId))
								.addChild("Entry", slug)
								.getKey());
					} else {
						String title = slug;
						DropboxFile file = dropbox.getFile(deltaEntry.path);
						String html = markdownProcessor.markdown(file.contents);
						Document doc = Jsoup.parse(html);
						Elements h1 = doc.select("h1");
						if (!h1.isEmpty()) {
							title = h1.first().text();
						}
						Entity e = new Entity("Entry", entryKey, blogger.getKey());
						e.setProperty("title", title);
						e.setProperty("slug", slug);
						e.setProperty("published", file.metadata.client_mtime.date);
						e.setProperty("updated", file.metadata.modified.date);
						e.setUnindexedProperty("contentMarkdown", new Text(file.contents));
						e.setUnindexedProperty("contentHtml", new Text(html));
						entitiesToInsert.add(e);
					}
				}
			}
			if (entriesCursor == null || !entriesCursor.equals(delta.cursor)) {
				blogger.setProperty("entriesCursor", delta.cursor);
				rePutBlogger = true;
			}
		} catch (IOException e) {
			logger.info("io error while syncing with dropbox: " + e.getMessage());
			return SyncStatus.FAILED;
		} catch (DropboxServiceException e) {
			logger.info("dropbox API error: " + e.getMessage());
			if (e.mustReauthenticate) {
				return SyncStatus.UNAUTHORISED;
			}
			return SyncStatus.FAILED;
		}
		
		Transaction txn = null;
		try {
			txn = datastoreService.beginTransaction();
			datastoreService.put(txn, entitiesToInsert);
			datastoreService.delete(txn, keysToDelete);
			if (rePutBlogger) {
				datastoreService.put(txn, blogger);
			}
			txn.commit();
		} catch (IllegalStateException e) {
			if (txn != null) {
				txn.rollback();
			}
			logger.info("IllegalStateException: " + e.getMessage());
			return SyncStatus.FAILED;
		} catch (DatastoreFailureException e) {
			if (txn != null) {
				txn.rollback();
			}
			logger.info("DatastoreFailureException: " + e.getMessage());
			return SyncStatus.FAILED;
		} catch (ConcurrentModificationException e) {
			// TODO a retry mechanism
			logger.info("ConcurrentModificationException: " + e.getMessage());
			return SyncStatus.FAILED;
		}
		return SyncStatus.OK;
	}
}
