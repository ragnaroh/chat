package com.ragnaroh.chat.server.web.servlet.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ragnaroh.chat.server.services.RoomService;
import com.ragnaroh.chat.server.services.model.room.RoomLite;

@RestController
@RequestMapping("/rooms")
public class RoomsController {

   @Autowired
   private RoomService roomService;

   @GetMapping
   public List<RoomLite> getRooms() {
      return roomService.getRoomsLite();
   }

   @PostMapping("/create")
   public String createRoom(@RequestBody String name) {
      return roomService.createRoom(name);
   }

}
