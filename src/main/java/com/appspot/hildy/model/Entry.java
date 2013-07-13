package com.appspot.hildy.model;

import com.google.appengine.api.datastore.Text;

public class Entry {
	public String title;
	public String slug;
	public Text contentMarkdown;
	public Text contentHtml;
	public String contentHtmlValue;
	public MuDate published;
	public MuDate updated;
	public String entryKey;
}
