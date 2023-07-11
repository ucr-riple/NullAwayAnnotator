/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.checkers.ucrtaint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.CheckerBaseClass;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.region.Region;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents <a href="https://github.com/kanaksad/UCRTaintingChecker">UCRTaint</a> checker in
 * Annotator.
 */
public class UCRTaint extends CheckerBaseClass<UCRTaintError> {

  /** The name of the checker. This is used to identify the checker in the configuration file. */
  public static final String NAME = "UCRTaint";

  public UCRTaint(Context context) {
    super(context);
  }

  @Override
  public void preprocess() {}

  @Override
  public Set<UCRTaintError> deserializeErrors(ModuleInfo module) {
    ImmutableSet<Path> paths =
        module.getModuleConfiguration().stream()
            .map(moduleInfo -> moduleInfo.dir.resolve("errors.json"))
            .collect(ImmutableSet.toImmutableSet());
    Set<UCRTaintError> errors = new HashSet<>();
    paths.forEach(
        path -> {
          try {
            String content = Files.readString(path, Charset.defaultCharset());
            content = "{ \"errors\": [" + content.substring(0, content.length() - 1) + "]}";
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(content);
            JSONArray errorsJson = (JSONArray) jsonObject.get("errors");
            errorsJson.forEach(o -> errors.add(deserializeErrorFromJSON((JSONObject) o, module)));
          } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
          }
        });
    return errors;
  }

  private UCRTaintError deserializeErrorFromJSON(JSONObject errorsJson, ModuleInfo moduleInfo) {
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
              final ImmutableList.Builder<ImmutableList<Integer>> bul = ImmutableList.builder();
              AtomicBoolean empty = new AtomicBoolean(true);
              if (((JSONObject) fixJson.get("location")).containsKey("type-variable-position")) {
                JSONArray indecies =
                    (JSONArray)
                        ((JSONObject) fixJson.get("location")).get("type-variable-position");
                indecies.forEach(
                    index -> {
                      List<Integer> indexList = new ArrayList<>();
                      ((JSONArray) index).forEach(ii -> indexList.add(((Long) ii).intValue()));
                      bul.add(ImmutableList.copyOf(indexList));
                      empty.set(false);
                    });
              }
              if (empty.get()) {
                bul.add(ImmutableList.of(0));
              }
              location.ifField(onField -> extendVariableList(onField, moduleInfo));
              builder.add(
                  new Fix(
                      new AddTypeUseMarkerAnnotation(
                          location,
                          "edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted",
                          bul.build()),
                      errorType,
                      true));
            });
    return new UCRTaintError(errorType, "", region, offset, builder.build());
  }

  @Override
  public void suppressRemainingErrors() {
    throw new RuntimeException(
        "Suppression for remaining errors is not supported for " + NAME + "yet!");
  }

  @Override
  public void verifyCheckerCompatibility() {}

  @Override
  public void prepareConfigFilesForBuild(ImmutableSet<ModuleConfiguration> configurations) {
    configurations.forEach(
        module -> {
          DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
          try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Root
            Element rootElement = doc.createElement("serialization");
            doc.appendChild(rootElement);

            // Output dir
            Element outputDir = doc.createElement("path");
            outputDir.setTextContent(module.dir.toString());
            rootElement.appendChild(outputDir);

            // Writings
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(module.checkerConfig.toFile());
            transformer.transform(source, result);
          } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException("Error happened in writing config.", e);
          }
        });
  }

  @Override
  public UCRTaintError createError(
      String errorType,
      String errorMessage,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes,
      ModuleInfo moduleInfo) {
    return new UCRTaintError(errorType, errorMessage, region, offset, resolvingFixes);
  }
}
