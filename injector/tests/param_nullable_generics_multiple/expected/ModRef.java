package com.uber;
import javax.annotation.Nullable;
public class ModRef {
   public ModRef(
       IMethod method,
       Context context,
       AbstractCFG<?, ?> cfg,
       SSAInstruction[] instructions,
       SSAOptions options,
       @Nullable Map<Integer, ConstantValue> constants)
       throws AssertionError {
           super(
               method, 
               instructions,
               makeSymbolTable(method, instructions, constants, cfg),
               new SSACFG(method, cfg, instructions),
               options
           );
         if (PARANOID) { repOK(instructions); }
         setupLocationMap();
    }
}
