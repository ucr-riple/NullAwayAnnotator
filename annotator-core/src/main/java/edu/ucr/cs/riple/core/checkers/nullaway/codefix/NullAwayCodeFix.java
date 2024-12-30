package edu.ucr.cs.riple.core.checkers.nullaway.codefix;

import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.ChatGPT;
import edu.ucr.cs.riple.injector.Injector;

public class NullAwayCodeFix {

  private final ChatGPT gpt;
  private final Injector injector;

  public NullAwayCodeFix(Injector injector) {
    gpt = new ChatGPT();
    this.injector = injector;
  }

  public void fix(NullAwayError error) {
    switch (error.messageType) {
      case "DEREFERENCE_NULLABLE":
        resolveDereferenceError(error);
        break;
      default:
        return;
    }
  }

  private void resolveDereferenceError(NullAwayError error) {
    if (ASTUtil.isObjectEqualsMethod(error.getRegion().member)) {
      String[] enclosingMethod =
          injector.getRegionSourceCode(
              error.path, error.getRegion().clazz, error.getRegion().member);
    }
  }
}
