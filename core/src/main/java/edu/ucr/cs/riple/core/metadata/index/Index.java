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

package edu.ucr.cs.riple.core.metadata.index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Index<T extends Hashable> {

  private final HashMap<Integer, List<T>> items;
  private final Factory<T> factory;
  private final Path path;
  private final Index.Type type;
  public int total;

  public enum Type {
    BY_METHOD,
    BY_CLASS
  }

  public Index(Path path, Index.Type type, Factory<T> factory) {
    this.type = type;
    this.path = path;
    this.items = new HashMap<>();
    this.factory = factory;
    this.total = 0;
  }

  public void index() {
    items.clear();
    try (BufferedReader br = new BufferedReader(new FileReader(this.path.toFile()))) {
      String line;
      br.readLine();
      while ((line = br.readLine()) != null) {
        if (line.contains("'")) {
          while (true) {
            if (line.endsWith("null") || line.endsWith(")")) {
              int begin = line.indexOf("'");
              int end = line.lastIndexOf("'");
              line =
                  line.substring(0, begin)
                      + line.substring(begin, end).replaceAll("\t", "")
                      + line.substring(end);
              if (line.split("\t").length == 4) {
                break;
              }
            }
            line = line.trim() + br.readLine().trim();
          }
        }
        T item = factory.build(line.split("\t"));
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

  public List<T> getAllEntities() {
    List<T> ans = new ArrayList<>();
    items.forEach((integer, ts) -> ans.addAll(ts));
    return ans;
  }
}
