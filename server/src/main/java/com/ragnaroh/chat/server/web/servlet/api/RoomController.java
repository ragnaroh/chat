package com.ragnaroh.chat.server.web.servlet.api;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ragnaroh.chat.server.services.RoomService;

@RestController
@RequestMapping("/rooms/{id}")
public class RoomController {

   @Autowired
   private RoomService roomService;

   @GetMapping("/name")
   public String getRoomName(@PathVariable("id") String roomId) {
      return roomService.getRoom(roomId).getName();
   }

   @PostMapping("/try-enter")
   public boolean tryEnterRoom(@PathVariable("id") String roomId, HttpSession session) {
      return roomService.readdUser(roomId, session.getId());
   }

   @PostMapping("/enter")
   public boolean enterRoom(@PathVariable("id") String roomId, @RequestBody String username, HttpSession session) {
      return roomService.addUser(roomId, session.getId(), username);
   }

}
