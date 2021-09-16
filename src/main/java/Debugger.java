import edu.ucr.cs.riple.autofixer.AutoFixer;

public class Debugger {

  public static void main(String[] args) {
    String projectDir = "cd /Users/nima/Developer/TestNullAway";
    AutoFixer autoFixer = new AutoFixer();
    String runCommand = projectDir + " && ./gradlew build -x test";
    String dir = "/tmp/NullAwayFix";
    AutoFixer.DEPTH = 3;
    AutoFixer.NULLABLE_ANNOT = "javax.annotation.Nullable";
    autoFixer.start(runCommand, dir, false);
  }
}
