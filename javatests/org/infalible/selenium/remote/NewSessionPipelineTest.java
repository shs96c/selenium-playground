package org.infalible.selenium.remote;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.remote.json.Json.TO_JSON;
import static org.junit.Assert.assertEquals;

public class NewSessionPipelineTest {

  @Test
  public void spike() throws IOException {
    ActiveSession session = new FakeSession();

    NewSessionPipeline pipeline = NewSessionPipeline.builder()
        .match((caps, meta) -> () -> session)
        .build();


    byte[] rawPayload = TO_JSON.apply(new TreeMap<>()).getBytes(UTF_8);
    try (InputStream in = new ByteArrayInputStream(rawPayload);
         NewSessionPayload payload = new NewSessionPayload(in, rawPayload.length)) {
      ActiveSession result = pipeline.newSession(payload);

      assertEquals(session, result);
    }
  }

}
