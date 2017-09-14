package org.infalible.selenium.remote;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.infalible.selenium.remote.json.Json;
import org.junit.Test;
import org.openqa.selenium.SessionNotCreatedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NewSessionPayloadTest {

  @Test
  public void shouldExportTheJsonWireProtocolPayloadIfOnlyItemPresent() throws IOException {
    Map<String, Object> rawPayload = ImmutableMap.of(
        "desiredCapabilities", ImmutableMap.of(
            "browserName", "cheese"));

    List<PayloadSection> sections = asSections(rawPayload);

    assertEquals(1, sections.size());
    assertTrue(sections.get(0).getMetadata().isEmpty());
    assertEquals("cheese", sections.get(0).getCapabilities().get("browserName"));
  }

  @Test
  public void shouldExportAlwaysMatchPayloadIfOnlyItemPresent() throws IOException {
    List<PayloadSection> sections = asSections(
        ImmutableMap.of(
            "capabilities",
            ImmutableMap.of("alwaysMatch", ImmutableMap.of("browserName", "cheese"))));

    assertEquals(1, sections.size());
    assertEquals(ImmutableMap.of("browserName", "cheese"), sections.get(0).getCapabilities());
  }

  @Test
  public void shouldExportFirstMatchPayloadIfOnlyItemPresent() throws IOException {
    List<PayloadSection> sections = asSections(
        ImmutableMap.of(
            "capabilities", ImmutableMap.of(
                "firstMatch", ImmutableList.of(
                    ImmutableMap.of("browserName", "cheese"),
                    ImmutableMap.of("browserName", "peas")
                ))));

    assertEquals(2, sections.size());
    assertEquals(ImmutableMap.of("browserName", "cheese"), sections.get(0).getCapabilities());
    assertEquals(ImmutableMap.of("browserName", "peas"), sections.get(1).getCapabilities());
  }

  @Test
  public void shouldMergeW3CPayloads() throws IOException {
    List<PayloadSection> sections = asSections(
        ImmutableMap.of(
            "capabilities", ImmutableMap.of(
                "alwaysMatch", ImmutableMap.of("pageLoadStrategy", "eager"),
                "firstMatch", ImmutableList.of(
                    ImmutableMap.of("browserName", "cheese"),
                    ImmutableMap.of("browserName", "peas")
                ))));

    assertEquals(
        ImmutableList.of(
            ImmutableMap.of("browserName", "cheese", "pageLoadStrategy", "eager"),
            ImmutableMap.of("browserName", "peas", "pageLoadStrategy", "eager")),
        sections.stream().map(PayloadSection::getCapabilities).collect(ImmutableList.toImmutableList()));
  }

  @Test
  public void theW3CSpecSaysWeOnlyNeedACapabilitiesKey() throws IOException {
    ImmutableMap<String, Object> rawPayload = ImmutableMap.of("capabilities", ImmutableMap.of());

    List<PayloadSection> sections = asSections(rawPayload);

    assertEquals(1, sections.size());
    assertEquals(ImmutableMap.of(), sections.get(0).getCapabilities());
  }

  @Test(expected = SessionNotCreatedException.class)
  public void shouldValidateAllW3CPayloadsEvenIfNotUsed() throws IOException {
    asSections(ImmutableMap.of("capabilities", ImmutableMap.of(
        "alwaysMatch", ImmutableMap.of("chromeOptions", ImmutableMap.of()))));
  }

  @Test(expected = SessionNotCreatedException.class)
  public void duplicateKeysInTheW3CPayloadAreABadThing() throws IOException {
    asSections(ImmutableMap.of("capabilities", ImmutableMap.of(
        "alwaysMatch", ImmutableMap.of("browserName", "cheese"),
        "firstMatch", ImmutableList.of(ImmutableMap.of("browserName", "peas")))));
  }

  @Test
  public void shouldOutputOssCapabilitiesFirst() throws IOException {
    List<PayloadSection> sections = asSections(ImmutableMap.of(
        "capabilities", ImmutableMap.of(
            "alwaysMatch", ImmutableMap.of("browserName", "cheese")),
        "desiredCapabilities", ImmutableMap.of("browserName", "peas")));

    assertEquals(
        ImmutableList.of(
            ImmutableMap.of("browserName", "peas"),
            ImmutableMap.of("browserName", "cheese")),
        sections.stream().map(PayloadSection::getCapabilities).collect(ImmutableList.toImmutableList()));
  }

  @Test
  public void shouldSpoolVeryLargeBlobsToDiskRatherThanUsingAllAvailableMemory() {

  }

  @Test
  public void shouldPreserveMetaData() throws IOException {
    Map<String, Object> rawPayload = ImmutableMap.of(
        "cloud:token", "i like cheese",
        "desiredCapabilities", ImmutableMap.of());

    asSections(rawPayload).stream()
        .map(PayloadSection::getMetadata)
        .filter(meta -> !"i like cheese".equals(meta.get("cloud:token")))
        .findAny()
        .ifPresent(meta -> fail("Meta data not correct: " + meta));
  }

  private List<PayloadSection> asSections(Map<String, Object> rawPayload) throws IOException {
    byte[] bytes = Json.TO_JSON.apply(rawPayload).getBytes(UTF_8);

    try (NewSessionPayload payload = new NewSessionPayload(new ByteArrayInputStream(bytes), bytes.length)) {
      return payload.stream().collect(ImmutableList.toImmutableList());
    }
  }
}