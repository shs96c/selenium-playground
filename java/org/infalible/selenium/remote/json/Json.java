package org.infalible.selenium.remote.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;

public class Json {
  private final static Gson GSON = new GsonBuilder().serializeNulls().setLenient().create();
  public static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

  public final static Function<Object, String> TO_JSON = GSON::toJson;

  public final static Function<String, Map<String, Object>> TO_MAP = string -> GSON.fromJson(string, MAP_TYPE);
}
