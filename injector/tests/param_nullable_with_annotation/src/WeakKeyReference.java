package com.uber;
class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {
   private final int hashCode;
   public WeakKeyReference(@Nullable K key, ReferenceQueue<K> queue) {
     super(key, queue);
     hashCode = System.identityHashCode(key);
   }
}
