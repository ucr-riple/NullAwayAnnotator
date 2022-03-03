package edu.ucr.cs.css;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

@AutoService(BugChecker.class)
@BugPattern(
    name = "NullAway",
    altNames = {"CheckNullabilityTypes"},
    summary = "Nullability type error.",
    tags = BugPattern.StandardTags.LIKELY_ERROR,
    severity = WARNING)
public class CSS extends BugChecker
    implements BugChecker.MethodInvocationTreeMatcher,
        BugChecker.AssignmentTreeMatcher,
        BugChecker.MemberSelectTreeMatcher {
  
  private final Config config;

  public CSS() {
    this.config = new Config();
  }

  public CSS(ErrorProneFlags flags) {
    this.config = new Config(flags);
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    if (config.fieldTrackerIsActive) {
      Symbol expressionSym = ASTHelpers.getSymbol(tree.getExpression());
      if (expressionSym != null && expressionSym.getKind() == ElementKind.FIELD) {
        config.WRITER.saveFieldTrackerNode(tree.getExpression(), state);
      }
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (config.fieldTrackerIsActive && symbol.getKind().equals(ElementKind.FIELD)) {
      config.WRITER.saveFieldTrackerNode(tree, state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (config.methodTrackerIsActive) {
      config.WRITER.saveMethodTrackerNode(tree.getMethodSelect(), state);
    }
    return Description.NO_MATCH;
  }
}
