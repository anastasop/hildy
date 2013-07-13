package com.appspot.hildy.model.dropbox;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class DropboxDeltaEntry {
	public String path;
	public DropboxFileMetadata metadata;
	
	public DropboxDeltaEntry(String path, DropboxFileMetadata metadata) {
		this.path = path;
		this.metadata = metadata;
	}
	
	public static class DropboxDeltaEntrySerializer implements JsonSerializer<DropboxDeltaEntry> {
		@Override
		public JsonElement serialize(DropboxDeltaEntry entry, Type type, JsonSerializationContext ctxt) {
			JsonArray a = new JsonArray();
			a.add(new JsonPrimitive(entry.path));
			a.add(ctxt.serialize(entry.metadata, DropboxFileMetadata.class));
			return a;
		}
	}
	
	public static class DropboxDeltaEntryDeserializer implements JsonDeserializer<DropboxDeltaEntry> {
		@Override
		public DropboxDeltaEntry deserialize(JsonElement elem, Type type, JsonDeserializationContext ctxt)
				throws JsonParseException {
			if (elem.isJsonArray()) {
				JsonArray a = elem.getAsJsonArray();
				if (a.size() == 2 && a.get(0).isJsonPrimitive() && (a.get(1).isJsonObject() || a.get(1).isJsonNull())) {
					String path = a.get(0).getAsString();
					DropboxFileMetadata metadata = ctxt.deserialize(a.get(1), DropboxFileMetadata.class);
					return new DropboxDeltaEntry(path, metadata);
				}
			}
			throw new JsonParseException("cannot deserialize DropboxDeltaEntry");
		}
	}
}
