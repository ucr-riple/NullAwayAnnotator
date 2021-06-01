package edu.ucr.cs.riple.diagnose.metadata;

import org.json.simple.JSONObject;

import java.util.Objects;

public class MethodInfo {

    public final int id;
    public final String method;
    public final String clazz;
    public final String uri;

    public MethodInfo(int id, JSONObject info){
        this.id = id;
        this.method = String.valueOf(info.get("method"));
        this.clazz = String.valueOf(info.get("class"));
        this.uri = String.valueOf(info.get("uri"));
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
