package org.infalible.selenium.remote.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonInput implements Closeable {
  private final Gson gson;
  private final JsonReader jsonReader;

  JsonInput(Gson gson, JsonReader jsonReader) {
    this.gson = gson;
    this.jsonReader = jsonReader;
  }

  @Override
  public void close() throws IOException {
    jsonReader.close();
  }

  public void beginObject() throws IOException {
    jsonReader.beginObject();
  }

  public void endObject() throws IOException {
    jsonReader.endObject();
  }

  public void beginArray() throws IOException {
    jsonReader.beginArray();
  }

  public void endArray() throws IOException {
    jsonReader.endArray();
  }

  public boolean hasNext() throws IOException {
    return jsonReader.hasNext();
  }

  public String nextName() throws IOException {
    return jsonReader.nextName();
  }

  public <T> T read(Type type) {
    T o = gson.fromJson(jsonReader, type);
    return o;
  }

}
