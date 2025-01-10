/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** A utility class for parsing JSON files. */
public class JsonParser {

  /** The JSON object to parse. */
  private final JsonObject json;

  /**
   * Creates a new JsonParser with the given path.
   *
   * @param path The path to the JSON file.
   */
  public JsonParser(Path path) {
    this.json = Utility.parseJson(path);
  }

  /**
   * Creates a new JsonParser with the given content.
   *
   * @param content The content of the JSON file.
   */
  public JsonParser(String content) {
    this.json = Utility.parseJson(content);
  }

  /**
   * Used to retrieve a value from a key in the JSON object, the given key should be in the format
   * of "key1:key2:key3". The method recursively searches for the key in the JSON object. It returns
   * a {@link OrElse} object that can be used to retrieve the value or a default value if the key
   * does not exist.
   *
   * @param key The key to search for in the JSON object.
   * @return An {@link OrElse} object that can be used to retrieve the value or a default value if
   *     the key does not exist.
   */
  public OrElse getValueFromKey(String key) {
    JsonObject current = json;
    List<String> keys = Arrays.asList(key.split(":"));
    while (keys.size() != 1) {
      if (current.keySet().contains(keys.get(0))) {
        current = (JsonObject) current.get(keys.get(0));
        keys.remove(0);
      } else {
        return new OrElse(JsonNull.INSTANCE);
      }
    }
    return current.keySet().contains(keys.get(0))
        ? new OrElse(current.get(keys.get(0)))
        : new OrElse(JsonNull.INSTANCE);
  }

  /**
   * Used to retrieve an array of values from a key in the JSON object, the given key should be in
   * the format of "key1:key2:key3". The method recursively searches for the key in the JSON object.
   * It returns a {@link ListOrElse} object that can be used to retrieve the value or a default
   * value if the key does not exist.
   *
   * @param key The key to search for in the JSON object.
   * @param mapper The function to map the JSON object to the desired type.
   * @return A {@link ListOrElse} object that can be used to retrieve the value or a default value
   *     if the key does not exist.
   * @param <T> The type of the array elements.
   */
  public <T> ListOrElse<T> getArrayValueFromKey(String key, Function<JsonObject, T> mapper) {
    OrElse jsonValue = getValueFromKey(key);
    if (jsonValue.value.equals(JsonNull.INSTANCE)) {
      return new ListOrElse<>(Stream.of());
    } else {
      if (jsonValue.value instanceof JsonArray) {
        return new ListOrElse<>(
            StreamSupport.stream(((JsonArray) jsonValue.value).spliterator(), false)
                .map(jsonElement -> mapper.apply(jsonElement.getAsJsonObject())));
      }
      throw new IllegalStateException(
          "Expected type to be json array, found: " + jsonValue.value.getClass());
    }
  }

  /**
   * A utility class to retrieve a value from a JSON object or a default value if the key does not
   * exist.
   */
  public static class OrElse {

    /** The JSON element retrieved from the JSON object. */
    private final JsonElement value;

    /**
     * Creates a new OrElse object with the given JSON element.
     *
     * @param value The retrieved JSON element.
     */
    private OrElse(JsonElement value) {
      this.value = value;
    }

    /**
     * Returns the value as a {@link JsonPrimitive} if it is not {@link JsonNull}, otherwise returns
     * the default value.
     *
     * @param defaultValue The default value to return if the key does not exist.
     * @return The value as a {@link JsonPrimitive} if it is not {@link JsonNull}, otherwise returns
     *     the default value.
     */
    public JsonPrimitive orElse(Object defaultValue) {
      return value.equals(JsonNull.INSTANCE)
          ? new JsonPrimitive(String.valueOf(defaultValue))
          : value.getAsJsonPrimitive();
    }
  }

  /**
   * A utility class to retrieve an array of values from a JSON object or a default value if the key
   * does not exist.
   *
   * @param <T> The type of the array elements.
   */
  public static class ListOrElse<T> {

    /** The stream of values retrieved from the JSON object. */
    private final Stream<T> value;

    /**
     * Creates a new ListOrElse object with the given stream of values.
     *
     * @param value The retrieved stream of values.
     */
    private ListOrElse(Stream<T> value) {
      this.value = value;
    }

    /**
     * Returns the values as a list if it is not null, otherwise returns the default value.
     *
     * @param defaultValue The default value to return if the key does not exist.
     * @return The values as a list if it is not null, otherwise returns the default value.
     */
    public List<T> orElse(List<T> defaultValue) {
      if (value == null) {
        return defaultValue;
      } else {
        return this.value.collect(Collectors.toList());
      }
    }
  }
}
