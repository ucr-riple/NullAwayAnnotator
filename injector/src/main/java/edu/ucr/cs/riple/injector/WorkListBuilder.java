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
  private List<Fix> fixes;

  public WorkListBuilder(String filePath) {
    this.filePath = filePath;
    readFixes();
  }

  private void readFixes() {
    try {
      BufferedReader bufferedReader =
          Files.newBufferedReader(Paths.get(filePath), Charset.defaultCharset());
      JSONObject obj = (JSONObject) new JSONParser().parse(bufferedReader);
      JSONArray fixesJson = (JSONArray) obj.get("fixes");
      bufferedReader.close();
      fixes = new ArrayList<>();
      for (Object o : fixesJson) {
        fixes.add(Fix.createFromJson((JSONObject) o));
      }
    } catch (FileNotFoundException ex) {
      throw new RuntimeException("Unable to open file: " + this.filePath);
    } catch (IOException ex) {
      throw new RuntimeException("Error reading file: " + this.filePath);
    } catch (ParseException e) {
      throw new RuntimeException("Error in parsing object: " + e);
    }
  }

  public WorkListBuilder(List<Fix> fixes) {
    if (fixes == null) {
      throw new RuntimeException("fix array cannot be null");
    }
    this.fixes = fixes;
  }

  public List<WorkList> getWorkLists() {
    ArrayList<String> uris = new ArrayList<>();
    ArrayList<WorkList> workLists = new ArrayList<>();
    for (Fix fix : this.fixes) {
      if (!new File(fix.uri).exists() && fix.uri.startsWith("file:")) {
        fix.uri = fix.uri.substring("file:".length());
      }
      if (!uris.contains(fix.uri)) {
        uris.add(fix.uri);
        WorkList workList = new WorkList(fix.uri);
        workLists.add(workList);
        workList.addFix(fix);
      } else {
        for (WorkList workList : workLists) {
          if (workList.getUri().equals(fix.uri)) {
            workList.addFix(fix);
            break;
          }
        }
      }
    }
    return workLists;
  }
}
