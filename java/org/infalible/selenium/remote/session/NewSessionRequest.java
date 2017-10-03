package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;
import org.infalible.function.ThrowingConsumer;
import org.infalible.selenium.json.Json;
import org.infalible.selenium.json.JsonOutput;
import org.infalible.selenium.w3c.W3CCapabilities;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.internal.JreHttpClient;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.json.Json.MAP_TYPE;

public class NewSessionRequest {

  private final Object source;
  private final ThrowingConsumer<JsonOutput> writeToStream;

  public NewSessionRequest(Map<String, Object> metadata, Capabilities caps) {
    this.source = Objects.requireNonNull(caps);

    this.writeToStream =
        json -> {
          json.beginObject();

          // Write metadata
          for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            json.name(entry.getKey()).write(entry.getValue(), Json.OBJECT_TYPE);
          }

          Map<String, ?> capsMap = caps.asMap();

          // Write JSON Wire protocol payload
          json.name("desiredCapabilities").write(capsMap, MAP_TYPE);

          // Write W3C payload
          json.name("capabilities").beginObject().name("firstMatch").beginArray();

          new JwpToW3CCapabilitiesAdapter()
              .apply(caps)
              .forEach(map -> json.write(map, MAP_TYPE));
          json.endArray().endObject();

          // Close the payload object
          json.endObject();
        };
  }

  public NewSessionRequest(W3CCapabilities caps) {
    this.source = Objects.requireNonNull(caps);

    this.writeToStream = json -> {
      json.beginObject();

      caps.getMetadata().forEach((key, value) -> json.name(key).write(value, Json.OBJECT_TYPE));
      json.name("capabilities").beginObject();

      json.name("alwaysMatch").write(caps.getAlwaysMatch().asMap(), Json.MAP_TYPE);

      json.name("firstMatch").beginArray();
      caps.getFirstMatches().forEach(cap -> json.write(cap.asMap(), Json.MAP_TYPE));
      json.endArray();

      json.endObject();
      json.endObject();
    };
  }

  public Result apply(HttpClient client) throws IOException {
    HttpRequest request = new HttpRequest(HttpMethod.POST, "/session");
    request.addHeader("Content-Type", "application/json; charset=utf-8");
    request.addHeader("Cache-Control", "no-cache");

    try (PipedInputStream in = new PipedInputStream(1024 * 1024)) {
      request.setContent(in);

      Set<Throwable> exception = new HashSet<>();
      Thread thread = new Thread(() -> {
        try (PipedOutputStream out = new PipedOutputStream(in);
             Writer writer = new OutputStreamWriter(out, UTF_8);
             JsonOutput json = Json.newOutput(writer)) {
          writeToStream.accept(json);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
      thread.setUncaughtExceptionHandler((t, e) -> exception.add(e));
      thread.setName("New Session request");
      thread.start();

      // Allow the pumps to drain
      long start = System.currentTimeMillis();
      HttpResponse response = client.execute(request, true);
      thread.join();
      long duration = System.currentTimeMillis() - start;
      if (!exception.isEmpty()) {
        throw exception.iterator().next();
      }

      Map<String, Object> parsed = Json.TO_MAP.apply(response.getContentString());

      Map.Entry<Dialect, Response> result =
          Stream.of(new W3CResponseConverter(), new OssResponseConverter())
              .map(converter -> converter.apply(parsed))
              .filter(Objects::nonNull)
              .findFirst()
              .orElseThrow(() -> new SessionNotCreatedException("Unable to create session for " + source));

      return new Result(result.getKey(), result.getValue());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SessionNotCreatedException("Interrupted when creating new session", e);
    } catch (Throwable e) {
      throw new SessionNotCreatedException(e.getMessage(), e);
    }
  }

  public static class Result {
    private final Dialect dialect;
    private final Response response;

    private Result(Dialect dialect, Response response) {
      this.dialect = dialect;
      this.response = response;
    }

    public Dialect getDialect() {
      return dialect;
    }

    public Response getResponse() {
      return response;
    }

    @Override
    public String toString() {
      return String.format("Dialect: %s -> %s", dialect, response.getValue());
    }
  }


  public static void main(String[] args) throws IOException {
    FirefoxOptions firefox = new FirefoxOptions();
    firefox.setHeadless(false);
    firefox.addPreference("foo.bar", "cheese");

    ChromeOptions chrome = new ChromeOptions();
    chrome.setHeadless(false);
    chrome.addArguments("--cheese");

    Capabilities caps = new MutableCapabilities()
        .merge(firefox)
        .merge(chrome)
        ;

    HttpClient client = new JreHttpClient.Factory().createClient(new URL("http://localhost:4444/wd/hub"));
    NewSessionRequest request = new NewSessionRequest(ImmutableMap.of(), caps);
    Result response = request.apply(client);

    System.out.println("response = " + response);
  }

}
