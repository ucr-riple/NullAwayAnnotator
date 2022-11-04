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
  public void visit(List<String> lines) {
    String line = lines.get(startPosition.line);
    String updated =
        line.substring(0, startPosition.column) + content + line.substring(endPosition.column);
    lines.set(startPosition.line, updated);
  }
}
