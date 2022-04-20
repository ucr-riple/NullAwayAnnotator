/*
 * Copyright (c) 2022 University of California, Riverside.
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

package edu.ucr.cs.riple.injector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WorkListBuilder {
  private String filePath;
  private List<Location> locations;

  public WorkListBuilder(String filePath) {
    this.filePath = filePath;
    readLocations();
  }

  private void readLocations() {
    try {
      BufferedReader bufferedReader =
          Files.newBufferedReader(Paths.get(filePath), Charset.defaultCharset());
      JSONObject obj = (JSONObject) new JSONParser().parse(bufferedReader);
      JSONArray locationsJson = (JSONArray) obj.get("locations");
      bufferedReader.close();
      locations = new ArrayList<>();
      for (Object o : locationsJson) {
        locations.add(Location.createFromJson((JSONObject) o));
      }
    } catch (FileNotFoundException ex) {
      throw new RuntimeException("Unable to open file: " + this.filePath);
    } catch (IOException ex) {
      throw new RuntimeException("Error reading file: " + this.filePath);
    } catch (ParseException e) {
      throw new RuntimeException("Error in parsing object: " + e);
    }
  }

  public WorkListBuilder(List<Location> locations) {
    if (locations == null) {
      throw new RuntimeException("location array cannot be null");
    }
    this.locations = locations;
  }

  public List<WorkList> getWorkLists() {
    ArrayList<String> uris = new ArrayList<>();
    ArrayList<WorkList> workLists = new ArrayList<>();
    for (Location location : this.locations) {
      if (!new File(location.uri).exists() && location.uri.startsWith("file:")) {
        location.uri = location.uri.substring("file:".length());
      }
      if (!uris.contains(location.uri)) {
        uris.add(location.uri);
        WorkList workList = new WorkList(location.uri);
        workLists.add(workList);
        workList.addLocation(location);
      } else {
        for (WorkList workList : workLists) {
          if (workList.getUri().equals(location.uri)) {
            workList.addLocation(location);
            break;
          }
        }
      }
    }
    return workLists;
  }
}
