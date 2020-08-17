package edu.ucr.cs.riple;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AnnotationEditor extends DefaultTask {

  @TaskAction
  public void AnnotEdit() {
    System.out.println("Annotation Editor Started...");
    AnnotationEditorExtension extension =
        getProject().getExtensions().findByType(AnnotationEditorExtension.class);
    if (extension == null) {
      extension = new AnnotationEditorExtension();
    }
    String[] remove = extension.getRemove();
    String[] srcSets = extension.getSrcSets();
    String[] subProjects = extension.getSubProjects();
    if (remove == null || remove.length == 0) {
      System.out.println("Nothing to remove, shutting down.");
      return;
    }

    System.out.println("Processing:");
    for (int i = 0; i < remove.length; i++) {
      System.out.println(i + ": " + remove[i]);
    }

    List<String> workList;
    if (subProjects != null) workList = Arrays.asList(subProjects);
    else workList = new ArrayList<>();

    for (Project p : getProject().getSubprojects()) {
      if (workList.size() == 0 || workList.contains(p.getPath().replace(":", ""))) {
        boolean processAll = false;
        String rootPath = p.getProjectDir().getPath() + "/src/";
        if (srcSets == null || srcSets.length == 0) {
          processAll = true;
        } else {
          for (int i = 0; i < srcSets.length; i++) {
            srcSets[i] = rootPath + srcSets[i];
          }
        }
        File[] directories = new File(rootPath).listFiles(File::isDirectory);
        if (directories == null) return;
        for (File f : directories) {
          for (String annot : remove) {
            if (shouldProcess(f, rootPath, processAll, srcSets)) process(f, annot);
          }
        }
      }
    }
    System.out.println("Finished.");
  }

  private void process(File root, String annot) {
    if (root.isDirectory()) {
      String[] paths;
      paths = root.list();
      if (paths == null) return;
      for (String path : paths) {
        File f = new File(root.getAbsolutePath() + "/" + path);
        if (f.isDirectory()) process(f, annot);
        else modifyFile(f, annot);
      }
    }
    modifyFile(root, annot);
  }

  private void modifyFile(File file, String annot) {
    String toRemove = annot;
    String[] names = annot.split("\\.");
    if(names.length > 1) toRemove = names[names.length - 1];
    if (!file.getAbsolutePath().endsWith(".java")) return;
    try {
      String text = readFile(file.getAbsolutePath(), Charset.defaultCharset());
      text = text.replaceAll("@" + toRemove, "");
      text = text.replaceAll("@" + annot, "");
      try {
        FileWriter myWriter = new FileWriter(file);
        myWriter.write(text);
        myWriter.close();
      } catch (IOException e) {
        System.out.println("An error occurred writing to file at: " + file.getAbsolutePath());
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean shouldProcess(File f, String rootPath, boolean processAll, String[] srcSets) {
    if (f.getAbsolutePath().equals(rootPath)) return false;
    if (processAll) return true;
    Collection<String> collection = Arrays.asList(srcSets);
    return collection.contains(f.getAbsolutePath());
  }

  static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }
}
