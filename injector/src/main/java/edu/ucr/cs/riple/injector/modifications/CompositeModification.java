package edu.ucr.cs.riple.injector.modifications;

import com.github.javaparser.Position;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.util.List;
import java.util.Set;

public class CompositeModification extends Modification {

  public CompositeModification(String content, Set<Position> positions) {
    super(content, positions.first());
  }

  @Override
  public void visit(List<String> lines, FileOffsetStore offsetStore) {}
}
