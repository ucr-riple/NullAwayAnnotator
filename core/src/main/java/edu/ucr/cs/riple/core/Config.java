package edu.ucr.cs.riple.core;

import com.google.common.base.Preconditions;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Config {
  public final boolean bailout;
  public final boolean optimized;
  public final boolean lexicalPreservationEnabled;
  public final boolean chain;
  public final boolean useCache;
  public final Path projectPath;
  public final Path dir;
  public final Path nullAwayConfigPath;
  public final Path cssConfigPath;
  public final String buildCommand;
  public final String nullableAnnot;
  public final int depth;

  public Config(String configPath) {
    Preconditions.checkNotNull(configPath);
    JSONObject jsonObject;
    try {
      Object obj =
          new JSONParser()
              .parse(Files.newBufferedReader(Paths.get(configPath), Charset.defaultCharset()));
      jsonObject = (JSONObject) obj;
    } catch (Exception e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + configPath, e);
    }
    this.depth = getValueFromKey(jsonObject, "DEPTH", Integer.class).orElse(1);
    this.chain = getValueFromKey(jsonObject, "CHAIN", Boolean.class).orElse(false);
    this.useCache = getValueFromKey(jsonObject, "CACHE", Boolean.class).orElse(true);
    this.lexicalPreservationEnabled =
        getValueFromKey(jsonObject, "FORMAT", Boolean.class).orElse(false);
    this.optimized = getValueFromKey(jsonObject, "OPTIMIZED", Boolean.class).orElse(true);
    this.bailout = getValueFromKey(jsonObject, "BAILOUT", Boolean.class).orElse(true);
    this.nullableAnnot =
        getValueFromKey(jsonObject, "ANNOTATION:NULLABLE", String.class)
            .orElse("javax.annotation.Nullable");
    this.nullAwayConfigPath =
        Paths.get(
            getValueFromKey(jsonObject, "NULLAWAY_CONFIG_PATH", String.class)
                .orElse("/tmp/NullAwayFix/config.xml"));
    this.cssConfigPath =
        Paths.get(
            getValueFromKey(jsonObject, "CSS_CONFIG_PATH", String.class)
                .orElse("/tmp/NullAwayFix/css.xml"));
    this.dir =
        Paths.get(
            getValueFromKey(jsonObject, "OUTPUT_DIR", String.class).orElse("/tmp/NullAwayFix"));
    this.projectPath =
        Paths.get(getValueFromKey(jsonObject, "PROJECT_PATH", String.class).orElse(null));
    Preconditions.checkNotNull(projectPath, "Project cannot be null");
    String command = getValueFromKey(jsonObject, "BUILD_COMMAND", String.class).orElse(null);
    Preconditions.checkNotNull(command, "Command to run NullAway cannot be null");
    this.buildCommand = "cd " + this.projectPath + " " + command;
  }

  static class OrElse<T> {
    final Object value;
    final Class<T> klass;

    OrElse(Object value, Class<T> klass) {
      this.value = value;
      this.klass = klass;
    }

    T orElse(T other) {
      return value == null ? other : klass.cast(this.value);
    }
  }

  private <T> OrElse<T> getValueFromKey(JSONObject json, String key, Class<T> klass) {
    if (json == null) {
      return new OrElse<>(null, klass);
    }
    try {
      ArrayList<String> keys = new ArrayList<>(Arrays.asList(key.split(":")));
      while (keys.size() != 1) {
        if (json.containsKey(keys.get(0))) {
          json = (JSONObject) json.get(keys.get(0));
          keys.remove(0);
        } else {
          return new OrElse<>(null, klass);
        }
      }
      return json.containsKey(keys.get(0))
          ? new OrElse<>(json.get(keys.get(0)), klass)
          : new OrElse<>(null, klass);
    } catch (Exception e) {
      return new OrElse<>(null, klass);
    }
  }
}
