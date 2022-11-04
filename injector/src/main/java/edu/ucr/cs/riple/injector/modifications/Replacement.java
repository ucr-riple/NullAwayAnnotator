package edu.ucr.cs.riple.injector.modifications;

import com.github.javaparser.Position;
import java.util.List;
import javax.lang.model.element.ElementKind;

public class Replacement extends Modification {

  private final Position endPosition;

  public Replacement(
      String content, Position startPosition, Position endPosition, ElementKind kind) {
    super(content, startPosition, kind);
    this.endPosition = new Position(endPosition.line - 1, endPosition.column - 1);
    if (content.equals("")) {
      throw new IllegalArgumentException("content cannot be empty, use Deletion instead");
    }
  }

  @Override
  public void visit(List<String> lines) {
    StringBuilder line = new StringBuilder(lines.get(startPosition.line));
    line.replace(startPosition.column, endPosition.column + 1, content);
    lines.set(startPosition.line, line.toString());
  }

  @Override
  public String toString() {
    return "{startPosition="
        + startPosition
        + "{endPosition="
        + endPosition
        + ", content='"
        + content
        + '\''
        + ", kind="
        + kind
        + '}';
  }
}
