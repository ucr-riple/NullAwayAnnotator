package edu.ucr.cs.riple.diagnose.metadata;


import java.util.Objects;

public class MethodInfo {

    public final long id;
    public final String method;
    public final String clazz;
    public final String uri;

    public MethodInfo(long id, String clazz, String method, String uri) {
        this.id = id;
        this.method = method;
        this.clazz = clazz;
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInfo)) return false;
        MethodInfo that = (MethodInfo) o;
        return method.equals(that.method) && clazz.equals(that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, clazz);
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "id=" + id +
                ", method='" + method + '\'' +
                ", clazz='" + clazz + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
