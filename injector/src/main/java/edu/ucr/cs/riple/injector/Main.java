package edu.ucr.cs.riple.injector;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

  //    public static void main(String[] args) throws IOException {
  //        // find paths all java files in the given directory
  //        String directory = "/Users/nima/Developer/logisim-evolution/src/main/java/com/cburch";
  //        try(Stream<Path> paths = Files.walk(Paths.get(directory))){
  //      paths.forEach(
  //          new Consumer<Path>() {
  //            @Override
  //            public void accept(Path path) {
  //              if (path.toString().endsWith(".java")) {
  //                try {
  //                  StaticJavaParser.setConfiguration(
  //                      new ParserConfiguration()
  //                          .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
  //                  StaticJavaParser.parse(path.toFile());
  //                } catch (Exception e) {
  //                  System.out.println(path);
  ////                  throw new RuntimeException(e);
  //                }
  //              }
  //            }
  //          });
  //        }
  //    }

  public static void main(String[] args) {
    Path path =
        Paths.get(
            "/Users/nima/Developer/logisim-evolution/src/main/java/com/cburch/logisim/tools/EditTool.java");
    StaticJavaParser.setConfiguration(
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
    try {
      StaticJavaParser.parse(path.toFile());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
