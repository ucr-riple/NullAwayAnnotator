package edu.ucr.cs.riple.editors;

import edu.riple.annotationinjector.Injector;
import edu.riple.annotationinjector.WorkListBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class ApplyBatch extends DefaultTask {

  @TaskAction
  public void applyBatch() {
    ApplyBatchExtension extension =
        getProject().getExtensions().findByType(ApplyBatchExtension.class);
    if (extension == null) {
      extension = new ApplyBatchExtension();
    }
    String batchPath = extension.getBatchPath();
    System.out.println("Batch Path: " + batchPath);
    Injector injector =
        Injector.builder()
            .setMode(Injector.MODE.BATCH)
            .setCleanImports(false)
            .build();

    System.out.println("Built Injector.");
    injector.start(new WorkListBuilder(batchPath).getWorkLists());
    System.out.println("Finished");
  }
}
