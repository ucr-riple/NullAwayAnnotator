package edu.ucr.cs.riple.injector.offsets;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileOffsetStore {

  private final Path path;
  private final List<Offset> offsets;
  private final List<String> lines;

  public FileOffsetStore(List<String> lines, Path path) {
    this.lines = lines;
    this.path = path;
    this.offsets = new ArrayList<>();
  }

  public void updateOffsetWithAddition(int line, int column, int dist) {
    int offset = characterOffsetAtLine(line);
    this.offsets.add(new Offset(offset + column, dist));
  }

  public void updateOffsetWithNewLineAddition(int line, int dist) {
    int offset = characterOffsetAtLine(line);
    this.offsets.add(new Offset(offset, dist + 1));
  }

  public void updateOffsetWithLineDeletion(int line) {
    int offset = characterOffsetAtLine(line);
    this.offsets.add(new Offset(offset, -lines.get(line).length() - 1));
  }

  public void updateOffsetWithDeletion(int line, int column, int dist) {
    int offset = characterOffsetAtLine(line);
    this.offsets.add(new Offset(offset + column, -1 * dist));
  }

  private int characterOffsetAtLine(int line) {
    int ans = 0;
    int current = 0;
    while (current <= line && current < lines.size()) {
      ans += lines.get(current).length();
      current += 1;
    }
    return ans;
  }

  public Path getPath() {
    return path;
  }

  public List<Offset> getOffSetsRelativeTo(List<Offset> relativeOffset) {
    return offsets.stream()
        .sorted(Comparator.comparingInt(o -> o.position))
        .map(offset -> offset.relativeTo(relativeOffset))
        .collect(Collectors.toList());
  }
}
