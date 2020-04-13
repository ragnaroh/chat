package com.ragnaroh.chat.server.common.exceptions;

public class IllegalInputException extends AbstractException {

   public IllegalInputException(String message, Object... args) {
      super(message, args);
   }

}
