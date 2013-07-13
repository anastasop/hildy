package com.appspot.hildy.model.dropbox;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class DropboxDate {
	public Date date;
	
	public DropboxDate() {}
	
	public DropboxDate(Date d) {
		this.date = d;
	}
	
	public static class DropboxDateTypeAdapter extends TypeAdapter<DropboxDate> {
		private static final String DROPBOX_DATE_FORMAT = "E, d MMM yyyy HH:mm:ss Z";
		private static final String DROPBOX_DATE_SAMPLE = "Sat, 21 Aug 2010 22:31:20 +0000";
		private SimpleDateFormat fmt = new SimpleDateFormat(DROPBOX_DATE_FORMAT, new Locale("EN"));
		
		@Override
		public DropboxDate read(JsonReader reader) throws IOException {
			String token = reader.nextString();
			try {
				Date d = fmt.parse(token);
				return new DropboxDate(d);
			} catch (ParseException e) {
				throw new JsonParseException(String.format(
						"date '%s' does not look like '%s'", token, DROPBOX_DATE_SAMPLE));
			}
		}

		@Override
		public void write(JsonWriter writer, DropboxDate date) throws IOException {
			if (date == null || date.date == null) {
				writer.nullValue();
			} else {
				writer.value(fmt.format(date.date));
			}
		}
	}
}
