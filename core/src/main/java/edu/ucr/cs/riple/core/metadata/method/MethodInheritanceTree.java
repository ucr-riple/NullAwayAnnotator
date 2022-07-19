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

package edu.ucr.cs.riple.core.metadata.method;

import edu.ucr.cs.riple.core.metadata.MetaData;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodInheritanceTree extends MetaData<MethodNode> {

  HashMap<Integer, MethodNode> nodes;
  private static int maxsize = 0;

  public MethodInheritanceTree(Path path) {
    super(path);
    final MethodNode top =
        new MethodNode(-1, "null", "null", Collections.emptyList(), -1, false, "null");
    nodes.put(-1, top);
  }

  @Override
  protected void setup() {
    super.setup();
    nodes = new HashMap<>();
  }

  @Override
  protected MethodNode addNodeByLine(String[] values) {
    Integer id = Integer.parseInt(values[0]);
    MethodNode node;
    if (nodes.containsKey(id)) {
      node = nodes.get(id);
    } else {
      node = new MethodNode();
      nodes.put(id, node);
    }
    Integer parentId = Integer.parseInt(values[3]);
    int size = Integer.parseInt(values[4]);
    if (size > maxsize) {
      maxsize = size;
    }
    node.fillInformation(id, values[1], values[2], parentId, size, Boolean.parseBoolean(values[6]));
    if (parentId != -1) {
      MethodNode parent = nodes.get(parentId);
      if (parent == null) {
        parent = new MethodNode();
        nodes.put(parentId, parent);
      }
      parent.addChild(id);
    }
    return node;
  }

  public MethodNode getClosestSuperMethod(String method, String clazz) {
    MethodNode node = findNode(method, clazz);
    if (node == null) {
      return null;
    }
    MethodNode parent = nodes.get(node.parent);
    return parent != null ? (nodes.get(parent.id) == null ? null : parent) : null;
  }

  public List<MethodNode> getSubMethods(String method, String clazz, boolean recursive) {
    List<MethodNode> ans = new ArrayList<>();
    MethodNode node = findNode(method, clazz);
    if (node == null) {
      return ans;
    }
    if (node.children == null) {
      return ans;
    }
    Set<Integer> workList = new HashSet<>(node.children);
    while (!workList.isEmpty()) {
      Set<Integer> tmp = new HashSet<>();
      for (Integer id : workList) {
        MethodNode selected = nodes.get(id);
        if (!ans.contains(selected)) {
          ans.add(selected);
          if (selected.children != null) {
            tmp.addAll(selected.children);
          }
        }
      }
      if (!recursive) {
        break;
      }
      workList.clear();
      workList.addAll(tmp);
    }
    return ans;
  }

  public MethodNode getSuperMethod(String method, String clazz) {
    MethodNode node = findNode(method, clazz);
    if (node == null) {
      return null;
    }
    MethodNode parent = nodes.get(node.parent);
    if (parent == null || parent.clazz == null || parent.clazz.equals("null")) {
      return null;
    }
    return parent;
  }

  public MethodNode findNode(String method, String clazz) {
    return findNode(
        candidate -> candidate.clazz.equals(clazz) && candidate.method.equals(method),
        method,
        clazz);
  }
}
