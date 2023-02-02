/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.too.scanner;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.example.too.scanner.out.ClassInfo;
import com.example.too.scanner.out.MethodInfo;
import com.example.too.scanner.out.TrackerNode;
import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ElementKind;

@AutoService(BugChecker.class)
@BugPattern(
    name = "AnnotatorScanner",
    altNames = {"TypeBasedStructureSerializer"},
    summary = "Serializes type-based metadata regarding code structure.",
    tags = BugPattern.StandardTags.STYLE,
    severity = SUGGESTION)
@SuppressWarnings("BugPatternNaming")
public class AnnotatorScanner extends BugChecker
    implements BugChecker.MethodInvocationTreeMatcher,
        BugChecker.MemberSelectTreeMatcher,
        BugChecker.MethodTreeMatcher,
        BugChecker.IdentifierTreeMatcher,
        BugChecker.VariableTreeMatcher,
        BugChecker.ClassTreeMatcher,
        BugChecker.NewClassTreeMatcher,
        BugChecker.LambdaExpressionTreeMatcher,
        BugChecker.MemberReferenceTreeMatcher {

  /**
   * Scanner context to store the state of the checker. Could not use {@link VisitorState#context}
   * included in Error Prone. See {@link ScannerContext} class for more info.
   */
  private final ScannerContext context;

  public AnnotatorScanner() {
    this.context = new ScannerContext(new DummyOptionsConfig());
  }

  public AnnotatorScanner(ErrorProneFlags flags) {
    this.context = new ScannerContext(new ErrorProneCLIFlagsConfig(flags));
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState visitorState) {
    if (!context.getConfig().classTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    context
        .getConfig()
        .getSerializer()
        .serializeClassInfo(
            new ClassInfo(
                ASTHelpers.getSymbol(classTree), visitorState.getPath().getCompilationUnit()));
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Config config = context.getConfig();
    if (!config.callTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    config
        .getSerializer()
        .serializeCallGraphNode(
            new TrackerNode(config, ASTHelpers.getSymbol(tree), state.getPath()));
    return Description.NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Config config = context.getConfig();
    config
        .getSerializer()
        .serializeCallGraphNode(
            new TrackerNode(config, ASTHelpers.getSymbol(tree), state.getPath()));
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Config config = context.getConfig();
    if (!config.methodTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    Symbol.MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
    serializeSymIfNonnull(methodSymbol);
    MethodInfo methodInfo = MethodInfo.findOrCreate(methodSymbol, context);
    methodInfo.findParent(state, context);
    methodInfo.setReturnTypeAnnotation(config);
    methodInfo.setURI(state);
    List<Boolean> paramAnnotations = new ArrayList<>();
    for (int i = 0; i < methodSymbol.getParameters().size(); i++) {
      paramAnnotations.add(SymbolUtil.paramHasNullableAnnotation(methodSymbol, i, config));
    }
    methodInfo.setAnnotationParameterFlags(paramAnnotations);
    config.getSerializer().serializeMethodInfo(methodInfo);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!context.getConfig().fieldTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    serializeSymIfField(ASTHelpers.getSymbol(tree.getInitializer()), state);
    serializeSymIfNonnull(ASTHelpers.getSymbol(tree));
    return Description.NO_MATCH;
  }

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    if (!context.getConfig().fieldTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    serializeSymIfField(ASTHelpers.getSymbol(tree), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (!context.getConfig().fieldTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    serializeSymIfField(ASTHelpers.getSymbol(tree), state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchLambdaExpression(
      LambdaExpressionTree lambdaExpressionTree, VisitorState visitorState) {
    Config config = context.getConfig();
    if (!config.callTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    serializeFunctionalInterface(config, lambdaExpressionTree, visitorState);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMemberReference(
      MemberReferenceTree memberReferenceTree, VisitorState visitorState) {
    Config config = context.getConfig();
    if (!config.callTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    serializeFunctionalInterface(config, memberReferenceTree, visitorState);
    if (memberReferenceTree instanceof JCTree.JCMemberReference) {
      Symbol calledMethod = ((JCTree.JCMemberReference) memberReferenceTree).sym;
      if (calledMethod instanceof Symbol.MethodSymbol) {
        context
            .getConfig()
            .getSerializer()
            .serializeCallGraphNode(new TrackerNode(config, calledMethod, visitorState.getPath()));
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Serializes a field usage if the received symbol is a field.
   *
   * @param symbol Received symbol.
   * @param state Error prone visitor state.
   */
  private void serializeSymIfField(Symbol symbol, VisitorState state) {
    if (symbol != null && symbol.getKind() == ElementKind.FIELD) {
      context
          .getConfig()
          .getSerializer()
          .serializeFieldGraphNode(new TrackerNode(context.getConfig(), symbol, state.getPath()));
    }
  }

  /**
   * Serializes the symbol if annotated with explicit {@code @Nonnull} annotations.
   *
   * @param symbol Given symbol to check its annotations.
   */
  private void serializeSymIfNonnull(Symbol symbol) {
    if (symbol instanceof Symbol.MethodSymbol) {
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
      for (int i = 0; i < methodSymbol.getParameters().size(); i++) {
        if (SymbolUtil.paramHasNonnullAnnotation(methodSymbol, i, context.getConfig())) {
          context
              .getConfig()
              .getSerializer()
              .serializeNonnullSym(methodSymbol.getParameters().get(i));
        }
      }
    }
    if ((symbol.getKind().equals(ElementKind.METHOD) || symbol.getKind().isField())
        && SymbolUtil.hasNonnullAnnotations(symbol, context.getConfig())) {
      context.getConfig().getSerializer().serializeNonnullSym(symbol);
    }
  }

  /**
   * Serializes the functional interface.
   *
   * @param tree Given tree.
   * @param state Visitor State.
   */
  private static void serializeFunctionalInterface(
      Config config, ExpressionTree tree, VisitorState state) {
    Symbol.MethodSymbol methodSym = SymbolUtil.getFunctionalInterfaceMethod(tree, state.getTypes());
    if (methodSym == null) {
      return;
    }
    config
        .getSerializer()
        .serializeCallGraphNode(new TrackerNode(config, methodSym, state.getPath()));
  }
}