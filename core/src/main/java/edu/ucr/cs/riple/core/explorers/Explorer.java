package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Report;

public interface Explorer {

  ImmutableSet<Report> explore();
}
