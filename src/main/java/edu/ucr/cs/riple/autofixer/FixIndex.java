package edu.ucr.cs.riple.autofixer;

import com.uber.nullaway.autofix.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FixIndex {

  public HashMap<Integer, List<Fix>> fixes;
  public int total;

  public FixIndex() {
    fixes = new HashMap<>();
    total = 0;
  }

  public void index() {
    try (BufferedReader br = new BufferedReader(new FileReader(Writer.ERROR))) {
      String line;
      String delimiter = Writer.getDelimiterRegex();
      br.readLine();
      while ((line = br.readLine()) != null) {
        Fix fix = Fix.fromCSVLine(line, delimiter);
        total++;
        int hash = Objects.hash(fix.className, fix.method);
        if (fixes.containsKey(hash)) {
          fixes.get(hash).add(fix);
        } else {
          List<Fix> newList = new ArrayList<>();
          newList.add(fix);
          fixes.put(hash, newList);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<Fix> getByClass(String clazz) {
    List<Fix> ans = fixes.get(Objects.hash(clazz));
    if (ans == null) {
      return Collections.emptyList();
    }
    return ans.stream().filter(error -> error.className.equals(clazz)).collect(Collectors.toList());
  }

  public List<Fix> getByMethod(String clazz, String method) {
    List<Fix> ans = fixes.get(Objects.hash(clazz, method));
    if (ans == null) {
      return Collections.emptyList();
    }
    return ans.stream()
        .filter(error -> error.className.equals(clazz) && error.method.equals(method))
        .collect(Collectors.toList());
  }
}
