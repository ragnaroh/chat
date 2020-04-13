package com.ragnaroh.chat.server.common.exceptions;

public class NotFoundException extends AbstractException {

   public NotFoundException(String message, Object... args) {
      super(message, args);
   }

}
