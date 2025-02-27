package edu.ucr.cs.riple.scanner.scanners;

import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashSet;
import java.util.Set;

public class AccumulatorScanner<T> extends TreeScanner<Set<Symbol>, T> {

  @Override
  public Set<Symbol> reduce(Set<Symbol> r1, Set<Symbol> r2) {
    if (r2 == null && r1 == null) {
      return Set.of();
    }
    Set<Symbol> combined = new HashSet<>();
    if (r1 != null) {
      combined.addAll(r1);
    }
    if (r2 != null) {
      combined.addAll(r2);
    }
    return combined;
  }
}
