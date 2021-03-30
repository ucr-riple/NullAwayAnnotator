import edu.ucr.cs.riple.diagnose.DiagnoseJar;

import java.util.Arrays;

public class Main {
  public static void main(String[] args) {
    DiagnoseJar diagnose = new DiagnoseJar();
    System.out.println("Number of received arguments: " + args.length);
    System.out.println("Actual Arguments: " + Arrays.toString(args));
    if (args.length != 2) {
      throw new RuntimeException(
          "Needs exactly two arguments: 1. command to execute NullAway, 2. path to the suggested fix file");
    }
    String fixPath = args[0];
    String runCommand = args[1];
    String dir = fixPath.contains("/") ? fixPath.substring(0, fixPath.lastIndexOf("/")) : "/";
    String diagnosePath = dir + "/diagnose.json";
    diagnose.start(runCommand, fixPath, diagnosePath);
  }
}
