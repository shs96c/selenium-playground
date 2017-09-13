package org.infalible.selenium.remote;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.remote.json.Json.TO_MAP;

public class NewSessionPayload implements Closeable {

  private final static Set<String> DEFINITELY_NOT_METADATA = ImmutableSet.of(
      "capabilities",
      "desiredCapabilities",
      "requiredCapabilities"
  );
  private final Map<String, Object> metadata;

  public NewSessionPayload(InputStream in, int estimatedLength) throws IOException {
    byte[] bytes = ByteStreams.toByteArray(in);
    Map<String, Object> completePayload = TO_MAP.apply(new String(bytes, UTF_8));

    // Pick out the pieces
    this.metadata = completePayload.entrySet().stream()
        .filter(entry -> Objects.nonNull(entry.getKey()))
        .filter(entry -> Objects.nonNull(entry.getValue()))
        .filter(entry -> !DEFINITELY_NOT_METADATA.contains(entry.getKey()))
        .collect(ImmutableSortedMap.toImmutableSortedMap(Ordering.natural(), Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public void close() throws IOException {
    // Do nothing
  }

  public Stream<PayloadSection> stream() {
    return Stream.of(new PayloadSection(new TreeMap<>(), metadata));
  }
}
