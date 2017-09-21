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

  public void write(JsonInput input, Type type) {
    Object read = input.read(type);
    gson.toJson(read, type, jsonWriter);
  }
}
