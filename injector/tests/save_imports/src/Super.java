package com.uber;
import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementBits;
import static com.ibm.wala.types.TypeName.PrimitiveMask;
import com.ibm.wala.types.TypeName.IntegerMask;
import com.ibm.wala.util.collections.HashMapFactory;
import java.io.Serializable;
import java.util.Map;
public class Super {
   Object test() {
       return new Object();
   }
}
