package edu.ucr.cs.css.out;

import com.google.common.base.Preconditions;
import com.google.errorprone.VisitorState;
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
    public final Symbol.MethodSymbol symbol;
    public final Symbol.ClassSymbol clazz;
    final int id;

    private Boolean[] annotFlags;
    private int parent = -1;
    private static int LAST_ID = 0;
    private static final Set<MethodInfo> discovered = new HashSet<>();

    private MethodInfo(Symbol.MethodSymbol method) {
        this.id = ++LAST_ID;
        this.symbol = method;
        this.clazz = (method != null) ? method.enclClass() : null;
        discovered.add(this);
    }

    public static MethodInfo findOrCreate(Symbol.MethodSymbol method) {
        Symbol.ClassSymbol clazz = method.enclClass();
        Optional<MethodInfo> optionalMethodInfo =
                discovered
                        .stream()
                        .filter(
                                methodInfo -> methodInfo.symbol.equals(method) && methodInfo.clazz.equals(clazz))
                        .findAny();
        return optionalMethodInfo.orElseGet(() -> new MethodInfo(method));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInfo)) return false;
        MethodInfo that = (MethodInfo) o;
        return symbol.equals(that.symbol) && clazz.equals(that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, clazz);
    }

    public void findParent(VisitorState state) {
        Symbol.MethodSymbol superMethod =
                SymbolUtil.getClosestOverriddenMethod(symbol, state.getTypes());
        if (superMethod == null || superMethod.toString().equals("null")) {
            this.parent = 0;
            return;
        }
        MethodInfo superMethodInfo = findOrCreate(superMethod);
        this.parent = superMethodInfo.id;
    }

    @Override
    public String toString() {
        Preconditions.checkArgument(symbol != null, "Should not be null at this point.");
        return id
                + "\t"
                + (clazz != null ? clazz : "null")
                + "\t"
                + symbol
                + "\t"
                + parent
                + "\t"
                + symbol.getParameters().size()
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

