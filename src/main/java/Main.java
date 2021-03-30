import edu.ucr.cs.riple.annotationinjector.Injector;
import edu.ucr.cs.riple.annotationinjector.WorkList;
import edu.ucr.cs.riple.annotationinjector.WorkListBuilder;
import edu.ucr.cs.riple.diagnose.DiagnoseJar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    if(args.length == 0){
      throw new RuntimeException("command not specified");
    }
    String command = args[0];
    switch (command){
      case "apply":
        apply(args);
        break;
      case "diagnose":
        diagnose(args);
        break;
      default:
        throw new RuntimeException("Unknown command: " + command);
    }
  }

  private static void diagnose(String[] args) {
    DiagnoseJar diagnose = new DiagnoseJar();
    System.out.println("Number of received arguments: " + args.length);
    System.out.println("Actual Arguments: " + Arrays.toString(args));
    if (args.length != 3) {
      throw new RuntimeException(
              "Diagnose needs exactly two arguments: 1. command to execute NullAway, 2. path to the suggested fix file");
    }
    String fixPath = args[1];
    String runCommand = args[2];
    String dir = fixPath.contains("/") ? fixPath.substring(0, fixPath.lastIndexOf("/")) : "/";
    String diagnosePath = dir + "/diagnose.json";
    diagnose.start(runCommand, fixPath, diagnosePath);
  }

  private static void apply(String[] args) {
    System.out.println("Number of received arguments: " + args.length);
    System.out.println("Actual Arguments: " + Arrays.toString(args));
    if (args.length != 2) {
      throw new RuntimeException(
              "Diagnose needs exactly one arguments: 1. path to the suggested fix file");
    }
    System.out.println("Building Injector...");
    Injector injector = Injector.builder().setMode(Injector.MODE.BATCH).setCleanImports(false).build();
    System.out.println("built.");
    System.out.println("Injecting...");
    injector.start(new WorkListBuilder(args[1]).getWorkLists());
    System.out.println("Finished");
  }
}
