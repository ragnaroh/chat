package com.ragnaroh.chat.server.web.servlet.api;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ragnaroh.chat.server.common.exceptions.IllegalInputException;
import com.ragnaroh.chat.server.common.exceptions.NotFoundException;
import com.ragnaroh.chat.server.services.RoomService;

@RestController
public class RoomController {

   @Autowired
   private RoomService roomService;

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

   @PostMapping("room")
   public String createRoom(@RequestBody String name) {
      return roomService.createRoom(name);
   }

   @GetMapping("room/{id}/name")
   public String getRoomName(@PathVariable("id") String roomId) {
      return roomService.getRoom(roomId).getName();
   }

   @GetMapping("room/{id}/username")
   public String getUsernameInRoom(@PathVariable("id") String roomId, HttpSession session) {
      String username = roomService.getRoomUsername(roomId, session.getId());
      return username == null ? "" : username;
   }

   @PostMapping("room/{id}/enter")
   public boolean enterRoom(@PathVariable("id") String roomId, @RequestBody String username, HttpSession session) {
      return roomService.addUser(roomId, session.getId(), username);
   }

   public static final class InputError {

      private final String field;
      private final String message;

      public InputError(String field, String message) {
         this.field = field;
         this.message = message;
      }

      public String getField() {
         return field;
      }

      public String getMessage() {
         return message;
      }

   }

}
