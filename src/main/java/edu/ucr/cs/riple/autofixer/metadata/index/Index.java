package edu.ucr.cs.riple.autofixer.metadata.index;

import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Index<T extends Hashable> {

  private final HashMap<Integer, List<T>> items;
  private final Factory<T> factory;
  private final String path;
  private final Index.Type type;
  public int total;

  public enum Type {
    BY_METHOD,
    BY_CLASS
  }

  public Index(String path, Index.Type type, Factory<T> factory) {
    this.type = type;
    this.path = path;
    this.items = new HashMap<>();
    this.factory = factory;
    this.total = 0;
  }

  public void index() {
    try (BufferedReader br = new BufferedReader(new FileReader(this.path))) {
      String line;
      String delimiter = Writer.getDelimiterRegex();
      br.readLine();
      while ((line = br.readLine()) != null) {
        T item = factory.build(line.split(delimiter));
        total++;
        int hash;
        if (type.equals(Index.Type.BY_CLASS)) {
          hash = Objects.hash(item.clazz);
        } else {
          hash = Objects.hash(item.clazz, item.method);
        }
        if (items.containsKey(hash)) {
          items.get(hash).add(item);
        } else {
          List<T> newList = new ArrayList<>();
          newList.add(item);
          items.put(hash, newList);
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("No output (error/fix) at this point");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<T> getByClass(String clazz) {
    List<T> ans = items.get(Objects.hash(clazz));
    if (ans == null) {
      return Collections.emptyList();
    }
    return ans.stream().filter(item -> item.clazz.equals(clazz)).collect(Collectors.toList());
  }

  public List<T> getByMethod(String clazz, String method) {
    List<T> ans = items.get(Objects.hash(clazz, method));
    if (ans == null) {
      return Collections.emptyList();
    }
    return ans.stream()
        .filter(item -> item.clazz.equals(clazz) && item.method.equals(method))
        .collect(Collectors.toList());
  }
}
