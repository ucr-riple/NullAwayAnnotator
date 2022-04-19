package edu.ucr.cs.riple.core;

import java.nio.file.Path;

public class Config {
  public boolean bailout;
  public boolean optimized;
  public boolean lexicalPreservationEnabled;
  public Path dir;
  public Path nullAwayConfigPath;
  public String buildCommand;
  public int depth;
  public String nullableAnnot;
  public boolean chain;
  public boolean useCache;
}
