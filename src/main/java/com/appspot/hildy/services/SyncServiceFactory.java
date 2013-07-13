package com.appspot.hildy.services;


public interface SyncServiceFactory {
	SyncService create(long bloggerId);
}
