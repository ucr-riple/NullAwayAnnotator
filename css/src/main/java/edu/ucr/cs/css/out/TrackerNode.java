package edu.ucr.cs.css.out;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

public class TrackerNode {
  private final Symbol member;
  private final Symbol.ClassSymbol callerClass;
  private final Symbol.MethodSymbol callerMethod;

  public TrackerNode(Symbol member, TreePath path) {
    ClassTree callerClass = ASTHelpers.findEnclosingNode(path, ClassTree.class);
    MethodTree callerMethod = ASTHelpers.findEnclosingNode(path, MethodTree.class);
    this.member = member;
    this.callerClass = (callerClass != null) ? ASTHelpers.getSymbol(callerClass) : null;
    this.callerMethod = (callerMethod != null) ? ASTHelpers.getSymbol(callerMethod) : null;
  }

  @Override
  public String toString() {
    if (callerClass == null) {
      return null;
    }
    return callerClass
            + "\t"
            + ((callerMethod == null) ? "null" : callerMethod)
            + "\t"
            + member
            + "\t"
            + ((member.enclClass() == null) ? "null" : member.enclClass());
  }

  public static String header() {
    return "CALLER_CLASS"
        + '\t'
        + "CALLER_METHOD"
        + '\t'
        + "MEMBER"
        + '\t'
        + "CALLEE_CLASS";
  }
}
