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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * The top-down tree structure of methods in the target module, where each method's parent node, is
 * their immediate overriding method.
 */
public class MethodDeclarationTree extends MetaData<MethodNode> {

  /** Each method has a unique id across all methods. This hashmap, maps ids to nodes. */
  private HashMap<Integer, MethodNode> nodes;

  /** Set of all classes flat name declared in module. */
  private HashSet<String> classNames;

  public MethodDeclarationTree(Path path) {
    super(path);
    // The root node of this tree with id: -1.
    nodes.put(-1, MethodNode.TOP);
  }

  @Override
  protected void setup() {
    super.setup();
    this.classNames = new HashSet<>();
    this.nodes = new HashMap<>();
  }

  @Override
  protected MethodNode addNodeByLine(String[] values) {
    // Nodes unique id.
    Integer id = Integer.parseInt(values[0]);
    MethodNode node;
    if (nodes.containsKey(id)) {
      node = nodes.get(id);
    } else {
      node = new MethodNode(id);
      nodes.put(id, node);
    }
    // Fill nodes information.
    Integer parentId = Integer.parseInt(values[3]);
    int size = Integer.parseInt(values[4]);
    node.fillInformation(
        new OnMethod(values[9], values[1], values[2]),
        parentId,
        size,
        Boolean.parseBoolean(values[6]),
        values[7],
        Boolean.parseBoolean(values[8]));
    // If node has a non-top parent.
    if (parentId != -1) {
      MethodNode parent = nodes.get(parentId);
      // If parent has not been seen visited before.
      if (parent == null) {
        parent = new MethodNode(parentId);
        nodes.put(parentId, parent);
      }
      // Parent is already visited.
      parent.addChild(id);
    }
    // Update list of all declared classes.
    this.classNames.add(node.location.clazz);
    return node;
  }

  /**
   * Locates the immediate super method of input method.
   *
   * @param method Method signature of input.
   * @param clazz Fully qualified name of the input method.
   * @return Corresponding node of the overridden method, null if method has no parent.
   */
  @Nullable
  public MethodNode getClosestSuperMethod(String method, String clazz) {
    MethodNode node = findNode(method, clazz);
    if (node == null) {
      return null;
    }
    MethodNode parent = nodes.get(node.parent);
    return parent.isNonTop() ? parent : null;
  }

  /**
   * Locates the overriding methods of input method.
   *
   * @param method Method signature of input.
   * @param clazz Fully qualified name of the input method.
   * @param recursive If ture, it will travers the declaration tree recursively.
   * @return ImmutableSet of overriding methods.
   */
  public ImmutableSet<MethodNode> getSubMethods(String method, String clazz, boolean recursive) {
    MethodNode node = findNode(method, clazz);
    if (node == null) {
      return ImmutableSet.of();
    }
    if (node.children == null) {
      return ImmutableSet.of();
    }
    Set<MethodNode> ans = new HashSet<>();
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
    return ImmutableSet.copyOf(ans);
  }

  /**
   * Locates a node based on the method signature and fully qualified class name.
   *
   * @param method Method signature.
   * @param clazz Fully Qualified name of the class.
   * @return Corresponding node.
   */
  public MethodNode findNode(String method, String clazz) {
    return findNodeWithHashHint(
        candidate ->
            candidate.location.clazz.equals(clazz) && candidate.location.method.equals(method),
        MethodNode.hash(method, clazz));
  }

  /**
   * Returns public method with public visibility and non-primitive return type.
   *
   * @return ImmutableSet of method nodes.
   */
  public ImmutableSet<MethodNode> getPublicMethodsWithNonPrimitivesReturn() {
    return findNodes(
            node ->
                node.hasNonPrimitiveReturn && node.visibility.equals(MethodNode.Visibility.PUBLIC))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Checks if the passed location, is targeting an element declared in the target module.
   *
   * @param location Location of the element.
   * @return true, if the passed location is targeting an element in the target module, and false
   *     otherwise.
   */
  public boolean declaredInModule(Location location) {
    return this.classNames.contains(location.clazz);
  }
}
