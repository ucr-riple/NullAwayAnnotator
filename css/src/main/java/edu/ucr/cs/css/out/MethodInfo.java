package edu.ucr.cs.css.out;

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.css.SymbolUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MethodInfo {
    final Symbol.MethodSymbol methodSymbol;
    final Symbol.ClassSymbol enclosingClass;
    final int id;

    Boolean[] annotFlags;
    int parent = -1;

    private static int LAST_ID = 0;
    static final Set<MethodInfo> discovered = new HashSet<>();
    private static final MethodInfo top = new MethodInfo(null, null);

    private MethodInfo(Symbol.MethodSymbol method, Symbol.ClassSymbol enclosingClass) {
        this.id = LAST_ID++;
        this.methodSymbol = method;
        this.enclosingClass = enclosingClass;
    }

    public static MethodInfo findOrCreate(Symbol.MethodSymbol method, Symbol.ClassSymbol clazz) {
        Optional<MethodInfo> optionalMethodInfo =
                discovered
                        .stream()
                        .filter(
                                methodInfo -> methodInfo.methodSymbol.equals(method) && methodInfo.enclosingClass.equals(clazz))
                        .findAny();
        if (optionalMethodInfo.isPresent()) {
            return optionalMethodInfo.get();
        }
        MethodInfo methodInfo = new MethodInfo(method, clazz);
        discovered.add(methodInfo);
        return methodInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInfo)) return false;
        MethodInfo that = (MethodInfo) o;
        return methodSymbol.equals(that.methodSymbol) && enclosingClass.equals(that.enclosingClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodSymbol, enclosingClass);
    }

    public void setParent(VisitorState state) {
        Symbol.MethodSymbol superMethod =
                SymbolUtil.getClosestOverriddenMethod(methodSymbol, state.getTypes());
        if (superMethod == null || superMethod.toString().equals("null")) {
            this.parent = top.id;
            return;
        }
        Symbol.ClassSymbol enclosingClass = ASTHelpers.enclosingClass(superMethod);
        MethodInfo superMethodInfo = findOrCreate(superMethod, enclosingClass);
        this.parent = superMethodInfo.id;
    }

    @Override
    public String toString() {
        if(methodSymbol == null){
            return null;
        }
        return id
                + "\t"
                + (enclosingClass != null ? enclosingClass : "null")
                + "\t"
                + methodSymbol
                + "\t"
                + parent
                + "\t"
                + methodSymbol.getParameters().size()
                + "\t"
                + Arrays.toString(annotFlags);
    }

    public static String header() {
        return "id" + "\t" + "class" + "\t" + "method" + "\t" + "parent" + "\t" + "size" + "\t" + "flags";
    }

    public void setParamAnnotations(List<Boolean> annotFlags) {
        if (annotFlags == null) {
            annotFlags = Collections.emptyList();
        }
        this.annotFlags = new Boolean[annotFlags.size()];
        this.annotFlags = annotFlags.toArray(this.annotFlags);
    }
}

