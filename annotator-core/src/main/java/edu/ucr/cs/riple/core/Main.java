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

import static java.nio.file.Files.newBufferedReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Starting point. */
public class Main {

  /**
   * Starting point.
   *
   * @param args if flag '--path' is found, all configurations will be set up based on the given
   *     json file, otherwise they will be set up according to the set of received cli arguments.
   */
  public static void main(String[] args) {
    //    Config config;
    //    if (args.length == 2 && args[0].equals("--path")) {
    //      config = new Config(Paths.get(args[1]));
    //    } else {
    //      config = new Config(args);
    //    }
    //    Annotator annotator = new Annotator(config);
    //    annotator.start();
    Path path = Paths.get("/home/nima/Developer/taint-benchmarks/commons-compress/src/main/java/");
    List<Path> result = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(path)) {
      result.addAll(
          walk.filter(Files::isRegularFile) // is a file
              .filter(p -> p.getFileName().toString().endsWith(".java"))
              .collect(Collectors.toList()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    for (Path path1 : result) {
      try {
        Files.readAllLines(path1, Charset.defaultCharset());
      } catch (IOException e) {
        System.out.println("CatchedError: " + e + " " + path1);
      }
    }

    //    String filePath = "/home/nima/Desktop/prob.java";
    //
    //    try (Reader reader = new BufferedReader(new InputStreamReader(new
    // FileInputStream(filePath), "UTF-8"))) {
    //      int character;
    //      int position = 1;
    //
    //      while ((character = reader.read()) != -1) {
    //        // Check for malformed character
    //        System.out.println("V: " + position + " " + (char) character);
    //        position++;
    //      }
    //    } catch (FileNotFoundException e) {
    //      System.err.println("File not found: " + e.getMessage());
    //    } catch (IOException e) {
    //      System.err.println("Error reading the file: " + e.getMessage());
    //    }
  }

  static List<String> read(Path path, Charset cs) throws IOException {
    BufferedReader reader = newBufferedReader(path, cs);

    List<String> var7 = null;
    List<String> result = new ArrayList();
    int number = 0;
    while (true) {
      String line = null;
      try {
        line = reader.readLine();
      } catch (IOException e) {
        System.out.println("CatchedError: " + e);
        throw new RuntimeException("Happened at path: " + path, e);
      }
      if (line == null) {
        var7 = result;
        break;
      }
      System.out.println(++number + " " + line);
      result.add(line);
    }
    reader.close();

    return var7;
  }

  //  public static void main(String[] args) {
  //    Path path = Paths.get("/tmp/ucr-tainting/compress/0/errors.json");;
  //    Set<UCRTaintError> errors = deserializeErrors(path);
  //    Set<Fix> resolvingFixes = Error.getResolvingFixesOfErrors(errors);
  //    final Injector injector = new Injector();
  //    injector.addAnnotations(resolvingFixes.stream().map(e ->
  // e.change).collect(Collectors.toSet()));
  //  }
  //
  //  public static Set<UCRTaintError> deserializeErrors(Path path) {
  //    Set<UCRTaintError> errors = new HashSet<>();
  //              try {
  //                String content = Files.readString(path, Charset.defaultCharset());
  //                content = "{ \"errors\": [" + content.substring(0, content.length() - 1) + "]}";
  //                JSONObject jsonObject = (JSONObject) new JSONParser().parse(content);
  //                JSONArray errorsJson = (JSONArray) jsonObject.get("errors");
  //                errorsJson.forEach(o -> errors.add(deserializeErrorFromJSON((JSONObject) o)));
  //              } catch (IOException | ParseException e) {
  //                throw new RuntimeException(e);
  //              }
  //    return errors;
  //  }
  //
  //  private static UCRTaintError deserializeErrorFromJSON(JSONObject errorsJson) {
  //    String errorType = (String) errorsJson.get("messageKey");
  //    int offset = ((Long) errorsJson.get("offset")).intValue();
  //    Region region =
  //            new Region(
  //                    (String) ((JSONObject) errorsJson.get("region")).get("class"),
  //                    (String) ((JSONObject) errorsJson.get("region")).get("symbol"));
  //    ImmutableSet.Builder<Fix> builder = ImmutableSet.builder();
  //    ((JSONArray) errorsJson.get("fixes"))
  //            .forEach(
  //                    o -> {
  //                      JSONObject fixJson = (JSONObject) o;
  //                      Location location =
  //                              Location.createLocationFromJSON((JSONObject)
  // fixJson.get("location"));
  //                      final ImmutableList.Builder<ImmutableList<Integer>> bul =
  // ImmutableList.builder();
  //                      AtomicBoolean empty = new AtomicBoolean(true);
  //                      if (((JSONObject)
  // fixJson.get("location")).containsKey("type-variable-position")) {
  //                        JSONArray indecies =
  //                                (JSONArray)
  //                                        ((JSONObject)
  // fixJson.get("location")).get("type-variable-position");
  //                        indecies.forEach(
  //                                index -> {
  //                                  List<Integer> indexList = new ArrayList<>();
  //                                  ((JSONArray) index).forEach(ii -> indexList.add(((Long)
  // ii).intValue()));
  //                                  bul.add(ImmutableList.copyOf(indexList));
  //                                  empty.set(false);
  //                                });
  //                      }
  //                      if (empty.get()) {
  //                        bul.add(ImmutableList.of(0));
  //                      }
  //                      builder.add(
  //                              new Fix(
  //                                      new AddTypeUseMarkerAnnotation(
  //                                              location,
  //
  // "edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted",
  //                                              bul.build()),
  //                                      errorType,
  //                                      true));
  //                    });
  //    return new UCRTaintError(errorType, "", region, offset, builder.build());
  //  }
}
