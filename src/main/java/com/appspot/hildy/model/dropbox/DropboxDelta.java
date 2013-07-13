package com.appspot.hildy.model.dropbox;

import java.util.ArrayList;
import java.util.List;

public class DropboxDelta {
	public List<DropboxDeltaEntry> entries;
	public Boolean reset;
	public String cursor;
	public Boolean has_more;
	
	public DropboxDelta() {
		this.entries = new ArrayList<DropboxDeltaEntry>();
		this.reset = false;
		this.cursor = null;
		this.has_more = false;
	}
}
