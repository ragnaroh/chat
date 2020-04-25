package com.ragnaroh.chat.server.web.servlet.websocket;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.ragnaroh.chat.server.services.RoomService;

@Controller
public class RoomTopicController {

   @Autowired
   private RoomService roomService;
   @Autowired
   private RoomSubscriptionHelper roomSubscriptionHelper;
   @Autowired
   private StompTemplate stompTemplate;

   @SubscribeMapping("/room/{id}")
   public RoomStompMessage onSubscribe(@DestinationVariable("id") String id,
                                       @Header("id") String subscriptionId,
                                       Principal principal) {
      roomSubscriptionHelper.onSubscribe(subscriptionId, principal.getName(), id);
      var event = roomService.activateUser(id, principal.getName());
      stompTemplate.sendToRoom(id, RoomStompMessage.event(event));
      stompTemplate.sendToRoom(id, RoomStompMessage.users(roomService.getActiveUsers(id)));
      var room = roomService.getRoom(id);
      return RoomStompMessage.initialData(room.getActiveUsers(), room.getEvents());
   }

   @MessageMapping("/room/{id}/message")
   @SendTo("/topic/room/{id}")
   public RoomStompMessage message(@DestinationVariable("id") String roomId,
                                   @Payload String message,
                                   Principal principal) {
      return RoomStompMessage.event(roomService.addMessage(roomId, principal.getName(), message));
   }

}
