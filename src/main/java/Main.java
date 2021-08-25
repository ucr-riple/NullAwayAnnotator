import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.util.Arrays;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      throw new RuntimeException("command not specified");
    }
    String command = args[0];
    switch (command) {
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
    AutoFixer autoFixer = new AutoFixer();
    System.out.println("Number of received arguments: " + args.length);
    System.out.println("Actual Arguments: " + Arrays.toString(args));
    if (!(args.length == 4 || args.length == 5)) {
      throw new RuntimeException(
          "AutoFixer needs two/three arguments: 1. command to execute NullAway, "
              + "2. output directory, 3. optimized [optional]");
    }
    boolean optimized = args.length == 5 && Boolean.getBoolean(args[4]);
    String dir = args[1];
    String runCommand = args[2];
    int depth = Integer.parseInt(args[3]);
    autoFixer.start(runCommand, dir, optimized, depth);
  }

  private static void apply(String[] args) {
    System.out.println("Number of received arguments: " + args.length);
    System.out.println("Actual Arguments: " + Arrays.toString(args));
    if (args.length != 2) {
      throw new RuntimeException(
          "AutoFixer needs exactly one arguments: 1. path to the suggested fix file");
    }
    System.out.println("Building Injector...");
    Injector injector = Injector.builder().setMode(Injector.MODE.BATCH).build();
    System.out.println("built.");
    System.out.println("Injecting...");
    injector.start(new WorkListBuilder(args[1]).getWorkLists());
    System.out.println("Finished");
  }
}
