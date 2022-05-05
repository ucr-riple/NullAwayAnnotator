package com.uber;
import javax.annotation.Nullable;
public class ModRef<T extends InstanceKey> {
   public Map<CGNode, OrdinalSet<PointerKey>> computeMod(
     CallGraph cg, PointerAnalysis<T> pa, @Nullable HeapExclusions heapExclude) {
     if (cg == null) {
       throw new IllegalArgumentException("cg is null");
     }
     Map<CGNode, Collection<PointerKey>> scan = scanForMod(cg, pa, heapExclude);
     return CallGraphTransitiveClosure.transitiveClosure(cg, scan);
   }
}
