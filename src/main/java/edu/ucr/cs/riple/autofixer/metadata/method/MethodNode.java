package edu.ucr.cs.riple.autofixer.metadata.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodNode {
  public List<Integer> children;
  public Integer parent;
  public Integer id;
  public String method;
  public String clazz;
  public String uri;
  public int size;
  public boolean[] annotFlags;

  void fillInformation(
      Integer id,
      String clazz,
      String method,
      String uri,
      Integer parent,
      int size,
      boolean[] annotFlags) {
    this.parent = parent;
    this.id = id;
    this.method = method;
    this.clazz = clazz;
    this.uri = uri;
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
        + ", uri='"
        + uri
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
        && Objects.equals(clazz, that.clazz)
        && Objects.equals(uri, that.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, clazz);
  }
}
