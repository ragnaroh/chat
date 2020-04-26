package com.ragnaroh.chat.server.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SynchronizedExecutor<L> {

   private final Map<L, CounterLock> locks = new ConcurrentHashMap<>();

   public void execute(L lock, Runnable action) {
      execute(lock, () -> {
         action.run();
         return null;
      });
   }

   public <R> R execute(L lock, Supplier<R> action) {
      CounterLock cl = locks.compute(lock, (k, v) -> v == null ? new CounterLock() : v.increment());
      synchronized (cl) {
         try {
            return action.get();
         } finally {
            if (cl.decrement() == 0) {
               // Only removes if key still points to the same value, to avoid issue described below.
               locks.remove(lock, cl);
            }
         }
      }
   }

   private static final class CounterLock {

      private AtomicInteger remaining = new AtomicInteger(1);

      private CounterLock increment() {
         // Returning a new CounterLock object if remaining = 0 to ensure that the lock is not removed in step 5 of the
         // following execution sequence:
         // 1) Thread 1 obtains a new CounterLock object from locks.compute (after evaluating "v == null" to true)
         // 2) Thread 2 evaluates "v == null" to false in locks.compute
         // 3) Thread 1 calls lock.decrement() which sets remaining = 0
         // 4) Thread 2 calls v.increment() in locks.compute
         // 5) Thread 1 calls locks.remove(key, lock)
         return remaining.getAndIncrement() == 0 ? new CounterLock() : this;
      }

      private int decrement() {
         return remaining.decrementAndGet();
      }
   }

}
