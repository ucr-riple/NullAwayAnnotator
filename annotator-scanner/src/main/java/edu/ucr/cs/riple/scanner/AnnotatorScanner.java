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

package edu.ucr.cs.riple.scanner;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.out.ClassInfo;
import edu.ucr.cs.riple.scanner.out.MethodInfo;
import edu.ucr.cs.riple.scanner.out.TrackerNode;
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
        BugChecker.ClassTreeMatcher {

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
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Config config = context.getConfig();
    if (!config.methodTrackerIsActive()) {
      return Description.NO_MATCH;
    }
    Symbol.MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
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
}
