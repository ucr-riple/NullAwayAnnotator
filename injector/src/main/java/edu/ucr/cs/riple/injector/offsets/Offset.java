package edu.ucr.cs.riple.injector.offsets;

import java.util.List;

public class Offset {
  public final int position;
  public final int dist;

  public Offset(int position, int dist) {
    this.position = position;
    this.dist = dist;
  }

  public static int adapt(int offset, List<Offset> existingOffsets) {
    if (existingOffsets == null) {
      return offset;
    }
    for (Offset current : existingOffsets) {
      if (offset > current.position) {
        offset -= current.dist;
      }
      if (offset < current.position) {
        break;
      }
    }
    return offset;
  }

  public Offset relativeTo(List<Offset> offsets) {
    return new Offset(adapt(this.position, offsets), dist);
  }
}
