package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MethodParamExplorer extends Explorer {

  MethodInheritanceTree mit;

  static class MethodParamNode {
    final int index;
    final String method;
    final String clazz;
    int referred;
    int effect;
    static HashMap<Integer, List<MethodParamNode>> nodes = new HashMap<>();

    private MethodParamNode(int index, String method, String clazz) {
      this.index = index;
      this.method = method;
      this.clazz = clazz;
    }

    public static MethodParamNode findOrCreate(int index, String method, String clazz) {
      int hash = Objects.hash(index, method, clazz);
      if (nodes.containsKey(hash)) {
        for (MethodParamNode candidate : nodes.get(hash)) {
          if (candidate.method.equals(method)
              && candidate.clazz.equals(clazz)
              && candidate.index == index) {
            return candidate;
          }
        }
        MethodParamNode newMethodParamNode = new MethodParamNode(index, method, clazz);
        nodes.get(hash).add(newMethodParamNode);
        return newMethodParamNode;
      }
      MethodParamNode newMethodParamNode = new MethodParamNode(index, method, clazz);
      List<MethodParamNode> newList = new ArrayList<>();
      newList.add(newMethodParamNode);
      nodes.put(hash, newList);
      return newMethodParamNode;
    }

    public static MethodParamNode find(int index, String method, String clazz) {
      int hash = Objects.hash(index, method, clazz);
      if (nodes.containsKey(hash)) {
        for (MethodParamNode candidate : nodes.get(hash)) {
          if (candidate.method.equals(method)
              && candidate.clazz.equals(clazz)
              && candidate.index == index) {
            return candidate;
          }
        }
      }
      return null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MethodParamNode)) return false;
      MethodParamNode methodParamNode = (MethodParamNode) o;
      return index == methodParamNode.index
          && method.equals(methodParamNode.method)
          && clazz.equals(methodParamNode.clazz);
    }

    @Override
    public int hashCode() {
      return Objects.hash(index, method, clazz);
    }

    @Override
    public String toString() {
      return "MethodParamNode{"
          + "index="
          + index
          + ", method='"
          + method
          + '\''
          + ", clazz='"
          + clazz
          + '\''
          + ", referred="
          + referred
          + ", effect="
          + effect
          + '}';
    }
  }

  public MethodParamExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
    mit = diagnose.methodInheritanceTree;
    makeAllNodes();
    measureNullSafetyAllMethods(diagnose, bank);
  }

  private void makeAllNodes() {
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(Writer.SUGGEST_FIX))) {
        String line;
        String delimiter = Writer.getDelimiterRegex();
        while ((line = br.readLine()) != null) {
          String[] infos = line.split(delimiter);
          if (!infos[0].equals("METHOD_PARAM")) {
            continue;
          }
          String clazz = infos[2];
          String method = infos[3];
          int index = Integer.parseInt(infos[5]);
          MethodParamNode methodParamNode = MethodParamNode.findOrCreate(index, method, clazz);
          methodParamNode.referred++;
        }
      }
    } catch (IOException e) {
      System.err.println("Exception happened in initializing MethodParamExplorer...");
    }
  }

  private void measureNullSafetyAllMethods(Diagnose diagnose, Bank bank) {
    int maxsize = MethodInheritanceTree.maxParamSize();
    System.out.println("Max size for method parameter list is: " + maxsize);
    for (int i = 0; i < maxsize; i++) {
      System.out.println("Analyzing params at index: " + i + " for all methods...");
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true)
              .setMethodParamTest(true, i);
      diagnose.buildProject(config);
      bank.saveState(false, true);
      for (List<MethodParamNode> list : MethodParamNode.nodes.values()) {
        int finalI = i;
        for (MethodParamNode methodParamNode :
            list.stream()
                .filter(methodParamNode -> methodParamNode.index == finalI)
                .collect(Collectors.toList())) {
          int localEffect =
              bank.compareByMethod(methodParamNode.clazz, methodParamNode.method, false);
          methodParamNode.effect =
              localEffect + calculateInheritanceViolationError(methodParamNode, i);
        }
      }
    }
    System.out.println("Captured all methods behavior against nullability of parameter.");
  }

  private int calculateInheritanceViolationError(MethodParamNode methodParamNode, int index) {
    int effect = 0;
    boolean[] thisMethodFlag =
        mit.findNode(methodParamNode.method, methodParamNode.clazz).annotFlags;
    if (index >= thisMethodFlag.length) {
      return 0;
    }
    for (MethodNode subMethod :
        mit.getSubMethods(methodParamNode.method, methodParamNode.clazz, false)) {
      if (!thisMethodFlag[index]) {
        if (!subMethod.annotFlags[index]) {
          effect++;
        }
      }
    }
    List<MethodNode> superMethods =
        mit.getSuperMethods(methodParamNode.method, methodParamNode.clazz, false);
    if (superMethods.size() != 0) {
      MethodNode superMethod = superMethods.get(0);
      if (!thisMethodFlag[index]) {
        if (superMethod.annotFlags[index]) {
          effect--;
        }
      }
    }
    return effect;
  }

  @Override
  public DiagnoseReport effect(Fix fix) {
    MethodParamNode methodParamNode =
        MethodParamNode.find(Integer.parseInt(fix.index), fix.method, fix.className);
    if (methodParamNode != null) {
      return new DiagnoseReport(fix, methodParamNode.effect - methodParamNode.referred);
    }
    return super.effect(fix);
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals("METHOD_PARAM");
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return false;
  }
}
