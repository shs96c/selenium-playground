package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Comparator;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.json.Json.TO_JSON;
import static org.junit.Assert.assertEquals;

public class NewSessionPipelineTest {

  @Test
  public void spike() throws IOException {
    ActiveSession session = new FakeSession("session");

    NewSessionPipeline pipeline =
        NewSessionPipeline.builder().match((caps, meta) -> () -> session).build();
    ActiveSession result = loadSession(pipeline);

    assertEquals(session, result);
  }

  @Test
  public void shouldReturnFirstMatchingProvider() throws IOException {
    ActiveSession expected = new FakeSession("expected");

    NewSessionPipeline pipeline =
        NewSessionPipeline.builder()
            .match((caps, meta) -> () -> null)
            .match((caps, meta) -> () -> expected)
            .match(
                (caps, meta) ->
                    () -> {
                      throw new RuntimeException("Never should be called");
                    })
            .build();

    ActiveSession result = loadSession(pipeline);

    assertEquals(expected, result);
  }

  @Test
  public void shouldAllowWeightingOfProviders() throws IOException {
    class WeightedSupplier implements Supplier<ActiveSession>, Comparable<Supplier<ActiveSession>> {

      private final ActiveSession session;
      private int freeSlots;

      public WeightedSupplier(ActiveSession session, int freeSlots) {
        this.session = session;
        this.freeSlots = freeSlots;
      }

      @Override
      public int compareTo(Supplier<ActiveSession> o) {
        return ((WeightedSupplier) o).freeSlots - freeSlots;
      }

      @Override
      public ActiveSession get() {
        return session;
      }
    }

    FakeSession expected = new FakeSession("expected");
    FakeSession unexpected = new FakeSession("unexpected");

    NewSessionPipeline pipeline =
        NewSessionPipeline.builder()
            .match((caps, meta) -> new WeightedSupplier(unexpected, 1))
            .match((caps, meta) -> new WeightedSupplier(expected, 5))
            .build();

    ActiveSession result = loadSession(pipeline);

    assertEquals(expected, result);
  }

  @Test
  public void shouldAllowCustomWeightingViaComparator() throws IOException {
    FakeSession expected = new FakeSession("expected");
    Supplier<ActiveSession> expectedSupplier = () -> expected;
    FakeSession unexpected = new FakeSession("unexpected");
    Supplier<ActiveSession> unexpectedSupplier = () -> unexpected;

    Comparator<Supplier<ActiveSession>> comparator =
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }
          if (o1 == expectedSupplier) {
            return -1;
          }
          if (o1 == unexpectedSupplier) {
            return 1;
          }

          return 0;
        };

    NewSessionPipeline pipeline =
        NewSessionPipeline.builder()
            .match((caps, meta) -> () -> unexpected)
            .match((caps, meta) -> unexpectedSupplier)
            .match((caps, meta) -> expectedSupplier)
            .match((caps, meta) -> unexpectedSupplier)
            .orderedBy(comparator)
            .build();
    ActiveSession result = loadSession(pipeline);

    assertEquals(expected, result);
  }

  private ActiveSession loadSession(NewSessionPipeline pipeline) throws IOException {
    String json = TO_JSON.apply(ImmutableMap.of("capabilities", ImmutableMap.of()));
    byte[] rawPayload = json.getBytes(UTF_8);

    try (Reader in = new StringReader(json);
        NewSessionPayload payload = new NewSessionPayload(in, rawPayload.length)) {
      return pipeline.newSession(payload);
    }
  }
}
