package org.infalible.selenium.remote;

import com.google.common.collect.ImmutableMap;
import org.infalible.selenium.remote.json.Json;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

public class NewSessionPayloadTest {

  @Test
  public void shouldExportTheJsonWireProtocolPayloadIfOnlyItemPresent() {
  }

  @Test
  public void shouldExportAlwaysMatchPayloadIfOnlyItemPresent() {

  }

  @Test
  public void shouldExportFirstMatchPayloadIfOnlyItemPresent() {

  }

  @Test
  public void shouldMergeW3CPayloads() {

  }

  @Test
  public void shouldValidateAllW3CPayloadsEvenIfNotUsed() {

  }

  @Test
  public void shouldOutputOssCapabilitiesFirst() {

  }

  @Test
  public void shouldSpoolVeryLargeBlobsToDiskRatherThanUsingAllAvailableMemory() {

  }

  @Test
  public void shouldPreserveMetaData() throws IOException {
    Map<String, Object> rawPayload = ImmutableMap.of(
        "cloud:token", "i like cheese",
        "desiredCapabilities", ImmutableMap.of());
    byte[] bytes = Json.TO_JSON.apply(rawPayload).getBytes(UTF_8);

    try (NewSessionPayload payload = new NewSessionPayload(new ByteArrayInputStream(bytes), bytes.length)) {
      payload.stream()
          .map(PayloadSection::getMetadata)
          .filter(meta -> !"i like cheese".equals(meta.get("cloud:token")))
          .findAny()
          .ifPresent(meta -> fail("Meta data not correct: " + meta));

    }
  }
}