package edu.ucr.cs.riple.editors;


import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
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
            .build();

    System.out.println("Built Injector.");
    injector.start(new WorkListBuilder(batchPath).getWorkLists());
    System.out.println("Finished");
  }
}
