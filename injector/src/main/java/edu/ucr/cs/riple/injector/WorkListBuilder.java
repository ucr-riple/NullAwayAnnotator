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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WorkListBuilder {
  private final Collection<Change> changes;

  public WorkListBuilder(String filePath) {
    changes = new ArrayList<>();
    BufferedReader reader;
    try {
      reader = Files.newBufferedReader(Paths.get(filePath), Charset.defaultCharset());
      String line = reader.readLine();
      while (line != null) {
        Change output = Change.fromArrayInfo(line.split("\\t"));
        changes.add(output);
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      throw new RuntimeException("Error happened in reading the outputs.", e);
    }
  }

  public WorkListBuilder(Collection<Change> changes) {
    if (changes == null) {
      throw new RuntimeException("location array cannot be null");
    }
    this.changes = changes;
  }

  public List<WorkList> getWorkLists() {
    ArrayList<String> uris = new ArrayList<>();
    ArrayList<WorkList> workLists = new ArrayList<>();
    for (Change change : this.changes) {
      if (!new File(change.location.uri).exists() && change.location.uri.startsWith("file:")) {
        change.location.uri = change.location.uri.substring("file:".length());
      }
      if (!uris.contains(change.location.uri)) {
        uris.add(change.location.uri);
        WorkList workList = new WorkList(change.location.uri);
        workLists.add(workList);
        workList.addLocation(change);
      } else {
        for (WorkList workList : workLists) {
          if (workList.getUri().equals(change.location.uri)) {
            workList.addLocation(change);
            break;
          }
        }
      }
    }
    return workLists;
  }
}
