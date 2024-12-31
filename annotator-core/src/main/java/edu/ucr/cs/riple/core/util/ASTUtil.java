package edu.ucr.cs.riple.core.util;

import com.github.javaparser.ast.CompilationUnit;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.Injector;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ASTUtil {

  public static boolean isObjectEqualsMethod(String member) {
    if (!member.contains("(")) {
      return false;
    }
    String methodName = member.substring(0, member.indexOf("("));
    // check if it has only one parameter and the parameter is an java.lang.Object
    String parameter = member.substring(member.indexOf("(") + 1, member.indexOf(")"));
    if (!parameter.equals("java.lang.Object")) {
      return false;
    }
    return methodName.equals("equals");
  }

  public static String[] getRegionSourceCode(Config config, Path path, Region region) {
    Injector injector = new Injector(config.languageLevel);
    return injector.getRegionSourceCode(path, region.clazz, region.member);
  }
}
