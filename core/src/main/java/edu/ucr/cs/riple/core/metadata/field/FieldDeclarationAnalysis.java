package edu.ucr.cs.riple.core.metadata.field;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Helper;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldDeclarationAnalysis extends MetaData<FieldDeclarationInfo> {

  public FieldDeclarationAnalysis(Path path) {
    super(path);
  }

  @Override
  protected FieldDeclarationInfo addNodeByLine(String[] values) {
    String clazz = values[0];
    String path = values[1];
    CompilationUnit tree;
    try {
      tree = StaticJavaParser.parse(new File(path));
      NodeList<BodyDeclaration<?>> members =
          Helper.getClassOrInterfaceOrEnumDeclarationMembersByFlatName(tree, clazz);
      if (members == null) {
        return null;
      }
      FieldDeclarationInfo info = new FieldDeclarationInfo(clazz);
      members.forEach(
          bodyDeclaration ->
              bodyDeclaration.ifFieldDeclaration(
                  fieldDeclaration -> {
                    NodeList<VariableDeclarator> vars =
                        fieldDeclaration.asFieldDeclaration().getVariables();
                    if (vars.size() > 1) {
                      info.addNewSetOfFieldDeclarations(
                          vars.stream()
                              .map(NodeWithSimpleName::getNameAsString)
                              .collect(Collectors.toSet()));
                    }
                  }));
      return info.size() == 0 ? null : info;
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  public Set<String> getInLineMultipleFieldDeclarationsOnField(Fix fix) {
    String clazz = fix.clazz;
    String field = fix.variable;
    if (!fix.change.kind.equals(FixType.FIELD.name)) {
      return new HashSet<>();
    }
    FieldDeclarationInfo candidate = findNode(node -> node.clazz.equals(clazz), clazz);
    if (candidate == null) {
      return Sets.newHashSet(field);
    }
    Optional<Set<String>> inLineGroupFieldDeclaration =
        candidate.fields.stream().filter(group -> group.contains(field)).findFirst();
    return inLineGroupFieldDeclaration.orElse(Sets.newHashSet(field));
  }
}
