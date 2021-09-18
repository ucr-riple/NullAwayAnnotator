package edu.ucr.cs.riple.autofixer.metadata.index;

import java.util.List;

public class Result<T> {

  public final int effect;
  public final List<T> dif;

  public Result(int effect, List<T> dif) {
    this.effect = effect;
    this.dif = dif;
  }
}
