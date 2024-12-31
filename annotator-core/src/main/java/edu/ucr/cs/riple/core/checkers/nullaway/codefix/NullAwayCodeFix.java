package edu.ucr.cs.riple.core.checkers.nullaway.codefix;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.ChatGPT;
import edu.ucr.cs.riple.core.util.ASTUtil;
import edu.ucr.cs.riple.injector.Injector;

public class NullAwayCodeFix {

  private final ChatGPT gpt;
  private final Injector injector;
  private final Config config;

  public NullAwayCodeFix(Config config, Injector injector) {
    this.gpt = new ChatGPT(config);
    this.injector = injector;
    this.config = config;
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
      gpt.fixDereferenceErrorInEqualsMethod(error);
    }
  }
}
