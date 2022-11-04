package edu.ucr.cs.riple.injector.modifications;

import com.github.javaparser.Position;
import java.util.List;
import javax.lang.model.element.ElementKind;

public class Replacement extends Modification {

  private final Position endPosition;

  public Replacement(
      String content, Position startPosition, Position endPosition, ElementKind kind) {
    super(content, startPosition, kind);
    this.endPosition = endPosition;
    if (content.equals("")) {
      throw new IllegalArgumentException("content cannot be empty, use Deletion instead");
    }
  }

  @Override
  public void visit(List<String> lines, Offset offset) {
    String line = lines.get(offset.line + startPosition.line);
    String updated =
        line.substring(0, offset.column + startPosition.column)
            + content
            + line.substring(offset.column + endPosition.column);
    lines.set(offset.line + startPosition.line, updated);
    offset.column -= endPosition.column - startPosition.column + content.length();
  }
}
