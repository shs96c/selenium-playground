package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.infalible.selenium.remote.json.Json;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.io.FileHandler;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

class DiskBackedPayloadView extends PayloadView implements Closeable {

  private final Set<String> keys;
  private final Path store;
  private final Map<String, Path> metadata;
  private final Path desiredCapabilities;
  private final Path alwaysMatch;
  private final Set<Path> firstMatch;
  private final AtomicBoolean firstMatchesRead = new AtomicBoolean(false);

  DiskBackedPayloadView(Reader in) throws IOException {
    store = Files.createTempDirectory("payload");
    desiredCapabilities = store.resolve("desired.json");
    alwaysMatch = store.resolve("always.json");
    ImmutableSet.Builder<Path> firstMatches = ImmutableSet.builder();
    ImmutableSet.Builder<String> keys = ImmutableSet.builder();
    ImmutableSortedMap.Builder<String, Path> metadata = ImmutableSortedMap.naturalOrder();

    Files.createDirectories(store);

    int metadataCount = 0;

    try (JsonReader jsonReader = Json.GSON.newJsonReader(in)) {
      jsonReader.beginObject();

      while (jsonReader.hasNext()) {
        String name = jsonReader.nextName();
        keys.add(name);

        switch (name) {
          case "desiredCapabilities":
            try (Writer writer = Files.newBufferedWriter(desiredCapabilities, UTF_8);
                 JsonWriter out = Json.GSON.newJsonWriter(writer)) {
              Json.GSON.toJson(Json.GSON.fromJson(jsonReader, Json.MAP_TYPE), Json.MAP_TYPE, out);
            }
            break;

          case "capabilities":
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
              String key = jsonReader.nextName();
              switch (key) {
                case "alwaysMatch":
                  Files.createDirectories(alwaysMatch.getParent());
                  try (Writer writer = Files.newBufferedWriter(alwaysMatch, UTF_8);
                       JsonWriter out = Json.GSON.newJsonWriter(writer)) {
                    Json.GSON.toJson(Json.GSON.fromJson(jsonReader, Json.MAP_TYPE), Json.MAP_TYPE, out);
                  }
                  break;

                case "firstMatch":
                  firstMatchesRead.set(true);
                  jsonReader.beginArray();
                  int count = 0;
                  while (jsonReader.hasNext()) {
                    Path match = store.resolve("firstMatch").resolve(count + ".json");
                    Files.createDirectories(match.getParent());
                    count++;
                    try (Writer writer = Files.newBufferedWriter(match, UTF_8);
                         JsonWriter out = Json.GSON.newJsonWriter(writer)) {
                      Json.GSON.toJson(Json.GSON.fromJson(jsonReader, Json.MAP_TYPE), Json.MAP_TYPE, out);
                    }
                    firstMatches.add(match);
                  }
                  jsonReader.endArray();
                  break;
              }
            }
            jsonReader.endObject();
            break;

          case "requiredCapabilities":
            // Older selenium versions sent this. We Don't care about it.
            break;

          default:
            Path path = store.resolve("metadata").resolve(metadataCount + ".json");
            Files.createDirectories(path.getParent());
            metadataCount++;
            try (Writer writer = Files.newBufferedWriter(path, UTF_8);
                 JsonWriter out = Json.GSON.newJsonWriter(writer)) {
              Json.GSON.toJson(Json.GSON.fromJson(jsonReader, Object.class), Object.class, out);
            }
            metadata.put(name, path);
            break;
        }
      }
      jsonReader.endObject();
    }

    this.keys = keys.build();
    this.firstMatch = firstMatches.build();
    this.metadata = metadata.build();
  }

  @Override
  Stream<String> getKeys() {
    return keys.stream();
  }

  @Override
  Entry getMetadata(String key) {
    Path path = metadata.get(key);
    if (path == null) {
      return null;
    }

    try (BufferedReader reader = Files.newBufferedReader(path, UTF_8)) {
      Object value = Json.GSON.fromJson(reader, Object.class);
      return new Entry(key, value);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }

  @Override
  Map<String, Object> getAlwaysMatch() {
    if (!Files.exists(alwaysMatch)) {
      return ImmutableMap.of();
    }
    try (Reader reader = Files.newBufferedReader(alwaysMatch, UTF_8)) {
      return Json.GSON.fromJson(reader, Json.MAP_TYPE);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }

  @Override
  Stream<Map<String, Object>> getFirstMatches() {
    if (!firstMatchesRead.get()) {
      return Stream.of(ImmutableMap.of());
    }
    
    return firstMatch.stream()
        .map(path -> {
          try (Reader reader = Files.newBufferedReader(path, UTF_8)) {
            return Json.GSON.fromJson(reader, Json.MAP_TYPE);
          } catch (IOException e) {
            throw new WebDriverException(e);
          }
        });
  }

  @Override
  Map<String, Object> getDesiredCapabilities() {
    if (!Files.exists(desiredCapabilities)) {
      return null;
    }
    try (Reader reader = Files.newBufferedReader(desiredCapabilities, UTF_8)) {
      return Json.GSON.fromJson(reader, Json.MAP_TYPE);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }

  @Override
  public void close() throws IOException {
    FileHandler.delete(store.toFile());
  }
}
