package com.uber;
import javax.annotation.Nullable;
import custom.aNullable;
public class Main {
   public enum Test{
     CLASSIC;
     @Nullable
     public Object run(){
       return null;
     }
   }
}
