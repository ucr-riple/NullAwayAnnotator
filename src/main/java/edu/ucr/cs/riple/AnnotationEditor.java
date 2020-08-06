package edu.ucr.cs.riple;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

public class AnnotationEditor extends DefaultTask {

  private String toRemove;
  private String remove;

  @TaskAction
  public void AnnotEdit() {
    System.out.println("Annotation Editor Started...");
    AnnotationEditorExtension extension =
        getProject().getExtensions().findByType(AnnotationEditorExtension.class);
    if (extension == null) {
      extension = new AnnotationEditorExtension();
    }
    remove = extension.getRemove();
    String[] srcSets = extension.getSrcSets();
    if (remove == null || remove.equals("")) {
      System.out.println("Nothing to remove, shutting down.");
      return;
    }
    String[] names = remove.split("\\.");
    if (names.length > 1) toRemove = names[names.length - 1];
    else toRemove = remove;

    boolean processAll = false;
    String rootPath = getProject().getProjectDir().getPath() + "/src/";

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
      if (shouldProcess(f, rootPath, processAll, srcSets)) process(f);
    }
    System.out.println("Finished.");
  }

  private void process(File root) {
    if (root.isDirectory()) {
      String[] paths;
      paths = root.list();
      if (paths == null) return;
      for (String path : paths) {
        File f = new File(root.getAbsolutePath() + "/" + path);
        if (f.isDirectory()) process(f);
        else modifyFile(f);
      }
    }
    modifyFile(root);
  }

  private void modifyFile(File file) {
    if (!file.getAbsolutePath().endsWith(".java")) return;
    try {
      String text = readFile(file.getAbsolutePath(), Charset.defaultCharset());
      text = text.replaceAll("import+\\s+" + remove + "+\\s*;", "");
      text = text.replaceAll("@" + toRemove, "");
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
