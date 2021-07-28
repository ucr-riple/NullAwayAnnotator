package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FixGraph;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public abstract class AdvancedExplorer extends BasicExplorer {

  final FixGraph fixGraph;

  public AdvancedExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
    fixGraph = new FixGraph();
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(Writer.SUGGEST_FIX))) {
        String line;
        String delimiter = Writer.getDelimiterRegex();
        while ((line = br.readLine()) != null) {
          Fix fix = Fix.fromCSVLine(line, delimiter);
          if (!isApplicable(fix)) {
            continue;
          }
          FixGraph.Node node = fixGraph.findOrCreate(fix);
          node.referred++;
        }
      }
    } catch (IOException e) {
      System.err.println("Exception happened in initializing MethodParamExplorer...");
    }
    init();
    explore();
  }

  protected abstract void explore();

  protected abstract void init();

  protected DiagnoseReport predict(Fix fix) {
    FixGraph.Node node = fixGraph.find(fix);
    if (node == null) {
      return null;
    }
    return new DiagnoseReport(fix, node.effect);
  }

  protected abstract DiagnoseReport effectByScope(Fix fix);

  @Override
  public DiagnoseReport effect(Fix fix) {
    DiagnoseReport report = predict(fix);
    if (report != null) {
      return report;
    }
    return effectByScope(fix);
  }
}
