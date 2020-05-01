package com.ragnaroh.chat.server.web.servlet.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ragnaroh.chat.server.services.RoomService;

@RestController
@RequestMapping("/rooms")
public class RoomsController {

   @Autowired
   private RoomService roomService;

   @PostMapping("/create")
   public String createRoom(@RequestBody String name) {
      return roomService.createRoom(name);
   }

}
