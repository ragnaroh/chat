package com.ragnaroh.chat.server.common.exceptions;

public abstract class AbstractException extends RuntimeException {

   protected AbstractException() {
      super();
   }

   protected AbstractException(String message, Object... args) {
      super(render(message, args));
   }

   private static String render(String message, Object... args) {
      String rendered = message;
      for (int i = 0; i < args.length; i++) {
         rendered = rendered.replaceFirst("\\{\\}", String.valueOf(args[i]));
      }
      return rendered;
   }

}
