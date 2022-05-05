package com.uber;
import javax.annotation.Nullable;
class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {
   private final int hashCode;
   public WeakKeyReference(@Nullable K key, @Nullable ReferenceQueue<K> queue) {
     super(key, queue);
     hashCode = System.identityHashCode(key);
   }
}
