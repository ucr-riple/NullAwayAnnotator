import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    Annotator annotator = new Annotator();
    if (args.length != 6) {
      throw new RuntimeException(
          "Annotator:diagnose needs 5 arguments: 1. command to execute NullAway, "
              + "2. output directory, 3. Annotator Depth level, 4. Nullable Annotation, 5. style");
    }
    Path dir = Paths.get(args[1]);
    String runCommand = args[2];
    annotator.DEPTH = Integer.parseInt(args[3]);
    annotator.nullableAnnot = args[4];
    annotator.KEEP_STYLE = Boolean.parseBoolean(args[5]);
    annotator.start(runCommand, dir, true);
  }

  private static void apply(String[] args) {
    if (args.length != 3) {
      throw new RuntimeException(
          "Annotator:apply needs two arguments: 1. path to the suggested fix file, 2. code style preservation flag");
    }
    boolean keepStyle = Boolean.parseBoolean(args[2]);
    System.out.println("Building Injector...");
    Injector injector =
        Injector.builder().setMode(Injector.MODE.BATCH).keepStyle(keepStyle).build();
    System.out.println("built.");
    List<Fix> fixes = Utility.readFixesJson(args[1]);
    System.out.println("Injecting...");
    injector.start(new WorkListBuilder(fixes).getWorkLists(), true);
    System.out.println("Finished");
  }
}
