package edu.ucr.cs.riple;

public class AnnotationEditorExtension {

    private String[] remove;
    private String[] srcSets;
    private String[] subProjects;

    public String[] getSubProjects() {
        return subProjects;
    }

    public void setSubProjects(String[] subProjects) {
        this.subProjects = subProjects;
    }

    public String[] getSrcSets() {
        return srcSets;
    }

    public void setSrcSets(String[] srcSets) {
        this.srcSets = srcSets;
    }

    public String[] getRemove() {
        return remove;
    }

    public void setRemove(String[] remove) {
        this.remove = remove;
    }
}
