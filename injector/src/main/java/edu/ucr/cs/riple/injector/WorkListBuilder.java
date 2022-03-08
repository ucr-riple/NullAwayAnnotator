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
