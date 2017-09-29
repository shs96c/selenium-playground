package org.infalible.selenium.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonOutput implements Closeable {
  private final Gson gson;
  private final JsonWriter jsonWriter;

  JsonOutput(Gson gson, JsonWriter jsonWriter) {
    this.gson = gson;
    this.jsonWriter = jsonWriter;
  }

  @Override
  public void close() throws IOException {
    jsonWriter.close();
  }

  public void write(JsonInput input, Type type) throws IOException {
    Object read = input.read(type);
    gson.toJson(read, type, jsonWriter);
  }

  public JsonOutput write(Object input, Type type) throws IOException {
    gson.toJson(input, type, jsonWriter);
    return this;
  }

  public JsonOutput beginObject() throws IOException {
    jsonWriter.beginObject();
    return this;
  }

  public JsonOutput endObject() throws IOException {
    jsonWriter.endObject();
    return this;
  }

  public JsonOutput name(String name) throws IOException {
    jsonWriter.name(name);
    return this;
  }

  public JsonOutput beginArray() throws IOException {
    jsonWriter.beginArray();
    return this;
  }

  public JsonOutput endArray() throws IOException {
    jsonWriter.endArray();
    return this;
  }
}
