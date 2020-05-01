package com.ragnaroh.chat.server.web.servlet.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ragnaroh.chat.server.common.exceptions.IllegalInputException;
import com.ragnaroh.chat.server.common.exceptions.NotFoundException;

@ControllerAdvice
public class CommonExceptionHandler {

   @ExceptionHandler
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   public String requestExceptionHandler(IllegalInputException exception) {
      return exception.getMessage();
   }

   @ExceptionHandler
   @ResponseStatus(HttpStatus.NOT_FOUND)
   public String requestExceptionHandler(NotFoundException exception) {
      return exception.getMessage();
   }

}
