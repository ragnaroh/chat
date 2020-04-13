package com.ragnaroh.chat.server.common;

public class Holder<T> {

   private T object;

   public Holder() {}

   public Holder(T object) {
      this.object = object;
   }

   public void set(T object) {
      this.object = object;
   }

   public T get() {
      return object;
   }

}
