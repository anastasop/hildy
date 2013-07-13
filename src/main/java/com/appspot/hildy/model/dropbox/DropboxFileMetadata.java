package com.appspot.hildy.model.dropbox;

import java.util.List;


public class DropboxFileMetadata {
	public String size;
	public Long bytes;
	public String path;
	public boolean is_dir = false;
	public boolean is_deleted = false;
	public String rev;
	public String hash;
	public boolean thumb_exists = false;
	public String icon;
	public DropboxDate modified;
	public DropboxDate client_mtime;
	public String root;
	public List<DropboxFileMetadata> contents;
	@Deprecated public String revision; // deprecated by dropbox
}
