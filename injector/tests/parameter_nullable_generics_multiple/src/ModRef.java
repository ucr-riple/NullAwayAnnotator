package com.uber;
public class ModRef {
   public ModRef(
       IMethod method,
       Context context,
       AbstractCFG<?, ?> cfg,
       SSAInstruction[] instructions,
       SSAOptions options,
       Map<Integer, ConstantValue> constants)
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
