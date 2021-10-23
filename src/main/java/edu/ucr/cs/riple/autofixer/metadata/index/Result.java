package edu.ucr.cs.riple.autofixer.metadata.index;

import java.util.List;

public class Result<T> {

  public final int size;
  public final List<T> dif;

  public Result(int size, List<T> dif) {
    this.size = size;
    this.dif = dif;
  }
}
