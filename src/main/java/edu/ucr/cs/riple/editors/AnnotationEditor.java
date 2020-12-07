package edu.ucr.cs.riple.editors;

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
    String[] subProjects = extension.getSubProjects();
    if (remove == null || remove.length == 0) {
      System.out.println("Nothing to remove, shutting down.");
      return;
    }

    System.out.println("Processing:");
    for (int i = 0; i < remove.length; i++) {
      System.out.println(i + ": " + remove[i]);
    }

    List<String> subProjectsList;
    if (subProjects != null) subProjectsList = Arrays.asList(subProjects);
    else subProjectsList = new ArrayList<>();

    Set<Project> workList = getProject().getSubprojects();
    if (subProjectsList.size() == 0 || workList.size() == 0) workList.add(getProject());
    for (Project p : workList) {
      if (subProjectsList.size() == 1 || subProjectsList.contains(p.getPath().replace(":", ""))) {
        String rootPath = p.getProjectDir().getPath();
        File[] directories = new File(rootPath).listFiles(File::isDirectory);
        if (directories == null) return;
        for (File f : directories) {
          for (String annot : remove) {
            if (shouldProcess(f, rootPath)) process(f, annot);
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

  private boolean shouldProcess(File f, String rootPath) {
    return !f.getAbsolutePath().equals(rootPath);
  }

  static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }
}
