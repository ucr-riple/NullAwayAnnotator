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
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.metadata.Registry;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * The top-down tree structure of methods in the target module, where each method's parent node, is
 * their immediate overriding method.
 */
public class MethodRegistry extends Registry<MethodRecord> {

  /** Each method has a unique id across all methods. This hashmap, maps ids to nodes. */
  private HashMap<Integer, MethodRecord> nodes;
  /** A map from class flat name to its declared constructors */
  private Multimap<String, MethodRecord> classConstructorMap;
  /** Set of all classes flat name declared in module. */
  private Set<String> declaredClasses;

  public MethodRegistry(Context context) {
    this(ImmutableSet.of(context.config.target));
  }

  public MethodRegistry(ImmutableSet<ModuleConfiguration> modules) {
    super(
        modules.stream()
            .map(moduleInfo -> moduleInfo.dir.resolve(Serializer.METHOD_INFO_FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()));
  }

  @Override
  protected void setup() {
    super.setup();
    this.declaredClasses = new HashSet<>();
    this.classConstructorMap = MultimapBuilder.hashKeys().hashSetValues().build();
    this.nodes = new HashMap<>();
    // The root node of this tree with id: 0.
    nodes.put(MethodRecord.TOP.id, MethodRecord.TOP);
  }

  @Override
  protected Builder<MethodRecord> getBuilder() {
    return values -> {
      // Nodes unique id.
      Integer id = Integer.parseInt(values[0]);
      MethodRecord node;
      if (nodes.containsKey(id)) {
        node = nodes.get(id);
      } else {
        node = new MethodRecord(id);
        nodes.put(id, node);
      }
      // Fill nodes information.
      Integer parentId = Integer.parseInt(values[3]);
      OnMethod location = new OnMethod(Helper.deserializePath(values[8]), values[1], values[2]);
      boolean isConstructor =
          Helper.extractCallableName(location.method).equals(Helper.simpleName(location.clazz));
      node.fillInformation(
          new OnMethod(Helper.deserializePath(values[8]), values[1], values[2]),
          parentId,
          Boolean.parseBoolean(values[5]),
          values[6],
          Boolean.parseBoolean(values[7]),
          isConstructor);
      // If node has a non-top parent.
      if (parentId > 0) {
        MethodRecord parent = nodes.get(parentId);
        // If parent has not been seen visited before.
        if (parent == null) {
          parent = new MethodRecord(parentId);
          nodes.put(parentId, parent);
        }
        // Parent is already visited.
        parent.addChild(id);
      }
      // Update list of all declared classes.
      declaredClasses.add(node.location.clazz);
      // If node is a constructor, add it to the list of constructors of its class.
      if (node.isConstructor) {
        classConstructorMap.put(node.location.clazz, node);
      }
      return node;
    };
  }

  /**
   * Returns the immediate super method of the given method.
   *
   * @param onMethod Method to find its super method.
   * @return Immediate super method of the given method, null if method has no super method.
   */
  @Nullable
  public MethodRecord getImmediateSuperMethod(OnMethod onMethod) {
    MethodRecord node = findMethodByName(onMethod.clazz, onMethod.method);
    if (node == null) {
      return null;
    }
    MethodRecord parent = nodes.get(node.parent);
    return (parent.isNonTop() && parent.location != null) ? parent : null;
  }

  /**
   * Returns the set of the immediate sub methods of the given method.
   *
   * @param onMethod Method to find its sub methods.
   * @return Immediate sub methods of the given method.
   */
  public ImmutableSet<MethodRecord> getImmediateSubMethods(OnMethod onMethod) {
    MethodRecord node = findMethodByName(onMethod.clazz, onMethod.method);
    if (node == null) {
      return ImmutableSet.of();
    }
    if (node.children == null) {
      return ImmutableSet.of();
    }
    return node.children.stream()
        .map(nodes::get)
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns the method corresponding to the given signature and class.
   *
   * @param encClass Fully Qualified name of the class.
   * @param method Method signature.
   * @return Corresponding method.
   */
  public MethodRecord findMethodByName(String encClass, String method) {
    return findRecordWithHashHint(
        candidate ->
            candidate.location.clazz.equals(encClass) && candidate.location.method.equals(method),
        MethodRecord.hash(method, encClass));
  }

  /**
   * Returns public method with public visibility and non-primitive return type.
   *
   * @return ImmutableSet of method nodes.
   */
  public ImmutableSet<MethodRecord> getPublicMethodsWithNonPrimitivesReturn() {
    return findRecords(MethodRecord::isPublicMethodWithNonPrimitiveReturnType)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns ImmutableSet of all constructors declared in the target module for the given class.
   *
   * @param clazz Flat name of the class.
   * @return ImmutableSet of all constructors declared in the target module for the given class.
   */
  public ImmutableSet<OnMethod> getConstructorsForClass(String clazz) {
    return classConstructorMap.get(clazz).stream()
        .map(node -> node.location)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Checks if the passed location is targeting an element declared in the target module.
   *
   * @param location Location of the element.
   * @return true, if the passed location is nonnull and is targeting an element in the target
   *     module, and false otherwise.
   */
  public boolean declaredInModule(@Nullable Location location) {
    if (location == null || location.clazz.equals("null")) {
      return false;
    }
    return this.declaredClasses.contains(location.clazz);
  }
}
