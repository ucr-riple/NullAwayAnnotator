package edu.ucr.cs.riple.core.metadata.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodNode {
  public List<Integer> children;
  public Integer parent;
  public Integer id;
  public String method;
  public String clazz;
  public int size;
  public boolean[] annotFlags;

  public MethodNode(
      int id,
      String clazz,
      String method,
      List<Integer> children,
      boolean[] annotFlags,
      int parent,
      String uri) {
    this.id = id;
    this.clazz = clazz;
    this.method = method;
    this.children = children;
    this.annotFlags = annotFlags;
    this.parent = parent;
  }

  public MethodNode() {}

  void fillInformation(
      Integer id, String clazz, String method, Integer parent, int size, boolean[] annotFlags) {
    this.parent = parent;
    this.id = id;
    this.method = method;
    this.clazz = clazz;
    this.size = size;
    this.annotFlags = annotFlags;
  }

  void addChild(Integer id) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(id);
  }

  @Override
  public String toString() {
    return "MethodNode{"
        + "children="
        + children
        + ", parent="
        + parent
        + ", id="
        + id
        + ", method='"
        + method
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + ", size="
        + size
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodNode)) return false;
    MethodNode that = (MethodNode) o;
    return Objects.equals(children, that.children)
        && Objects.equals(parent, that.parent)
        && Objects.equals(id, that.id)
        && Objects.equals(method, that.method)
        && Objects.equals(clazz, that.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, clazz);
  }
}
