/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.css;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.css.out.MethodInfo;
import edu.ucr.cs.css.out.TrackerNode;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

@AutoService(BugChecker.class)
@BugPattern(
    name = "CSS",
    altNames = {"TypeBasedStructureSerializer"},
    summary = "Serialized type based call/field graph.",
    tags = BugPattern.StandardTags.STYLE,
    severity = SUGGESTION)
public class CSS extends BugChecker
    implements BugChecker.MethodInvocationTreeMatcher,
        BugChecker.AssignmentTreeMatcher,
        BugChecker.MemberSelectTreeMatcher,
        BugChecker.ArrayAccessTreeMatcher,
        BugChecker.ReturnTreeMatcher,
        BugChecker.MethodTreeMatcher,
        BugChecker.VariableTreeMatcher,
        BugChecker.BinaryTreeMatcher,
        BugChecker.UnaryTreeMatcher,
        BugChecker.ConditionalExpressionTreeMatcher,
        BugChecker.IfTreeMatcher,
        BugChecker.WhileLoopTreeMatcher,
        BugChecker.ForLoopTreeMatcher,
        BugChecker.EnhancedForLoopTreeMatcher,
        BugChecker.LambdaExpressionTreeMatcher,
        BugChecker.IdentifierTreeMatcher,
        BugChecker.CompoundAssignmentTreeMatcher,
        BugChecker.SwitchTreeMatcher {

  private final Config config;

  public CSS() {
    this.config = new Config();
  }

  public CSS(ErrorProneFlags flags) {
    this.config = new Config(flags);
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    if (tree == null) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    serializeField(ASTHelpers.getSymbol(tree.getVariable()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!config.callTrackerIsActive) {
      return Description.NO_MATCH;
    }
    config.serializer.serializeCallGraphNode(
        new TrackerNode(ASTHelpers.getSymbol(tree), state.getPath()));
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!config.methodTrackerIsActive) {
      return Description.NO_MATCH;
    }
    Symbol.MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
    MethodInfo methodInfo = MethodInfo.findOrCreate(methodSymbol);
    methodInfo.findParent(state);
    List<Boolean> paramAnnotations = new ArrayList<>();
    for (int i = 0; i < methodSymbol.getParameters().size(); i++) {
      paramAnnotations.add(SymbolUtil.paramHasNullableAnnotation(methodSymbol, i, config));
    }
    methodInfo.setParamAnnotations(paramAnnotations);
    config.serializer.serializeMethodInfo(methodInfo);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree), state);
    serializeField(ASTHelpers.getSymbol(tree.getInitializer()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree), state);
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchArrayAccess(ArrayAccessTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree), state);
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree), state);
    serializeField(ASTHelpers.getSymbol(tree.getLeftOperand()), state);
    serializeField(ASTHelpers.getSymbol(tree.getRightOperand()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getVariable()), state);
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getCondition()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchForLoop(ForLoopTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getCondition()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getCondition()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    Symbol.MethodSymbol funcInterfaceMethod =
        SymbolUtil.getFunctionalInterfaceMethod(tree, state.getTypes());
    if (tree.getBodyKind() == LambdaExpressionTree.BodyKind.EXPRESSION
        && funcInterfaceMethod.getReturnType().getKind() != TypeKind.VOID) {
      serializeField(ASTHelpers.getSymbol(tree.getBody()), state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchUnary(UnaryTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getExpression()), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchWhileLoop(WhileLoopTree tree, VisitorState state) {
    if (!config.fieldTrackerIsActive) {
      return Description.NO_MATCH;
    }
    serializeField(ASTHelpers.getSymbol(tree.getCondition()), state);
    return Description.NO_MATCH;
  }

  private void serializeField(Symbol symbol, VisitorState state) {
    if (symbol != null && symbol.getKind() == ElementKind.FIELD) {
      config.serializer.serializeFieldGraphNode(new TrackerNode(symbol, state.getPath()));
    }
  }
}
