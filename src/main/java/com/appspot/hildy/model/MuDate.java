package com.appspot.hildy.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MuDate {
	private static final SimpleDateFormat ATOMFMT =
			new SimpleDateFormat("yyyy-M-d'T'HH:mm:ss+0000");
	private static final SimpleDateFormat TOCFMT =
			new SimpleDateFormat("d MMM yyyy");
	private Date date;
	
	public MuDate() {
		this.date = new Date();
	}
	
	public MuDate(Date date) {
		if (date != null) {
			this.date = new Date(date.getTime());
		} else {
			this.date = new Date();
		}
	}
	
	public Date getDate() {
		return date;
	}
	
	public String fmtAtom() {
		return ATOMFMT.format(date);
	}
	
	public String fmtToc() {
		return TOCFMT.format(date);
	}
	
	@Override
	public String toString() {
		return date.toString();
	}
}
