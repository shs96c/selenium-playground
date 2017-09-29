package org.infalible.selenium.remote.session;

import org.infalible.selenium.json.Json;
import org.infalible.selenium.json.JsonOutput;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
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
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.json.Json.MAP_TYPE;

public class Spike {

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
    Map<String, ?> capsMap = caps.asMap();

    PipedOutputStream out = new PipedOutputStream();
    new Thread(() -> {
        try (Writer writer = new OutputStreamWriter(out, UTF_8);
            JsonOutput json = Json.newOutput(writer)) {

          json.beginObject()
              .name("desiredCapabilities")
              .write(capsMap, MAP_TYPE)
              .name("capabilities")
              .beginObject()
              .name("firstMatch")
              .beginArray();

          new JwpToW3CCapabilitiesAdapter()
              .apply(caps)
              .forEach(
                  map -> {
                    try {
                      json.write(map, MAP_TYPE);
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  });

          json.endArray().endObject().endObject();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
    }).start();

    try (PipedInputStream in = new PipedInputStream(out, 1024 * 1024)) {
      HttpRequest request = new HttpRequest(HttpMethod.POST, "/session");
      request.addHeader("Content-Type", "application/json; charset=utf-8");
      request.addHeader("Cache-Control", "no-cache");
      request.setContent(in);

      HttpClient client = new JreHttpClient.Factory().createClient(new URL("http://localhost:4444/wd/hub"));
      long start = System.currentTimeMillis();
      HttpResponse response = client.execute(request, true);
      long time = System.currentTimeMillis() - start;

      // Ignore the content type, since we don't know if clients get this right.
      Map<String, Object> blob = Json.TO_MAP.apply(response.getContentString());

      Dialect dialect = Dialect.OSS;
      Response toReturn = new Response();
      if (blob.get("status") instanceof Number) {
        dialect = Dialect.OSS;
        toReturn = dialect.getResponseCodec().decode(response);
      } else if (blob.get("value") instanceof Map) {
        dialect = Dialect.W3C;

        @SuppressWarnings("unchecked") Map<String, Object> value = (Map<String, Object>) blob.get("value");

        if (value.get("capabilities") instanceof Map) {
          //noinspection unchecked
          toReturn.setValue(value.get("capabilities"));
          toReturn.setStatus(0);
        }
        if (value.get("sessionId") instanceof String) {
          toReturn.setSessionId((String) value.get("sessionId"));
        }
      }

      System.out.println(String.format("Dialect: %s --> %s", dialect, toReturn));
    }

  }
}
