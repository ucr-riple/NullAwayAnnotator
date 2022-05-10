package edu.ucr.cs.riple.core.metadata.field;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.injector.Helper;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FieldDeclarationAnalysis extends MetaData<Set<FieldDeclarationInfo>> {

  public FieldDeclarationAnalysis(Path path) {
    super(path);
  }

  @Override
  protected Set<FieldDeclarationInfo> addNodeByLine(String[] values) {
    String clazz = values[0];
    String path = values[1];
    CompilationUnit tree;
    Set<FieldDeclarationInfo> ans = new HashSet<>();
    try {
      tree = StaticJavaParser.parse(new File(path));
      NodeList<BodyDeclaration<?>> members =
          Helper.getClassOrInterfaceOrEnumDeclarationMembersByFlatName(tree, clazz);
      if (members == null) {
        return null;
      }
      members.forEach(
          bodyDeclaration ->
              bodyDeclaration.ifFieldDeclaration(
                  fieldDeclaration -> {
                    NodeList<VariableDeclarator> vars =
                        fieldDeclaration.asFieldDeclaration().getVariables();
                    if (vars.size() > 1) {
                      FieldDeclarationInfo info = new FieldDeclarationInfo(clazz);
                      vars.forEach(
                          variableDeclarator ->
                              info.fields.add(variableDeclarator.getNameAsString()));
                      ans.add(info);
                    }
                  }));
      return ans.size() == 0 ? null : ans;
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  public Set<String> getGroupFieldDeclarationsOnField(String clazz, String field) {
    Set<FieldDeclarationInfo> candidates =
        findNode(
            candidate ->
                candidate
                    .stream()
                    .anyMatch(fieldDeclarationInfo -> fieldDeclarationInfo.clazz.equals(clazz)),
            clazz);
    if (candidates == null) {
      return new HashSet<>();
    }
    Optional<FieldDeclarationInfo> info =
        candidates
            .stream()
            .filter(fieldDeclarationInfo -> fieldDeclarationInfo.containsField(field))
            .findFirst();
    return info.map(fieldDeclarationInfo -> fieldDeclarationInfo.fields).orElse(new HashSet<>());
  }
}
