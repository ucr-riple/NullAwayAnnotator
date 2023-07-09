/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.checkers.ucrtaint.UCRTaintError;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.region.Region;
import edu.ucr.cs.riple.injector.changes.AddFullTypeMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** Starting point. */
public class Main {

  //  /**
  //   * Starting point.
  //   *
  //   * @param args if flag '--path' is found, all configurations will be set up based on the given
  //   *     json file, otherwise they will be set up according to the set of received cli
  // arguments.
  //   */
  //  public static void main(String[] args) {
  //    System.out.println("NEW VERSION");
  //    Config config;
  //    if (args.length == 2 && args[0].equals("--path")) {
  //      config = new Config(Paths.get(args[1]));
  //    } else {
  //      config = new Config(args);
  //    }
  //    Annotator annotator = new Annotator(config);
  //    annotator.start();
  //  }

  public static void main(String[] args) {
    Path path = Paths.get("/tmp/ucr-tainting/opencms/0/errors.json");
    Set<UCRTaintError> errors = new HashSet<>();
    try {
      String content = Files.readString(path, Charset.defaultCharset());
      content = "{ \"errors\": [" + content.substring(0, content.length() - 1) + "]}";
      JSONObject jsonObject = (JSONObject) new JSONParser().parse(content);
      JSONArray errorsJson = (JSONArray) jsonObject.get("errors");
      errorsJson.forEach(o -> errors.add(deserializeErrorFromJSON((JSONObject) o)));
    } catch (IOException | ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static UCRTaintError deserializeErrorFromJSON(JSONObject errorsJson) {
    String errorType = (String) errorsJson.get("messageKey");
    int offset = ((Long) errorsJson.get("offset")).intValue();
    Region region =
        new Region(
            (String) ((JSONObject) errorsJson.get("region")).get("class"),
            (String) ((JSONObject) errorsJson.get("region")).get("symbol"));
    ImmutableSet.Builder<Fix> builder = ImmutableSet.builder();
    ((JSONArray) errorsJson.get("fixes"))
        .forEach(
            o -> {
              JSONObject fixJson = (JSONObject) o;
              Location location =
                  Location.createLocationFromJSON((JSONObject) fixJson.get("location"));
              builder.add(
                  new Fix(
                      new AddFullTypeMarkerAnnotation(
                          location, "edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted"),
                      errorType,
                      true));
            });
    return new UCRTaintError(errorType, "", region, offset, builder.build());
  }
}
