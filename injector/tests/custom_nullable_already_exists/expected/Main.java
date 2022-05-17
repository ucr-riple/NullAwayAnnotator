package com.uber;
import custom.Nullable;
public class Main {
   public enum Test{
     CLASSIC;
     @Nullable
     public Object run(){
       return null;
     }
   }
}
