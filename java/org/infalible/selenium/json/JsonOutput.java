package org.infalible.selenium.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
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

  public void write(JsonInput input, Type type) throws UncheckedIOException {
    Object read = input.read(type);
    gson.toJson(read, type, jsonWriter);
  }

  public JsonOutput write(Object input, Type type) throws UncheckedIOException {
    gson.toJson(input, type, jsonWriter);
    return this;
  }

  public JsonOutput beginObject() throws UncheckedIOException {
    try {
      jsonWriter.beginObject();
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonOutput endObject() throws UncheckedIOException {
    try {
      jsonWriter.endObject();
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonOutput name(String name) throws UncheckedIOException {
    try {
      jsonWriter.name(name);
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonOutput beginArray() throws UncheckedIOException {
    try {
      jsonWriter.beginArray();
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonOutput endArray() throws UncheckedIOException {
    try {
      jsonWriter.endArray();
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
