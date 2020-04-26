package com.ragnaroh.chat.server.web.servlet.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.ragnaroh.chat.server.services.RoomService;

@Controller
public class RoomTopicController {

   @Autowired
   private RoomService roomService;
   @Autowired
   private RoomSubscriptionHelper roomSubscriptionHelper;

   @SubscribeMapping("/room/{id}")
   public RoomStompMessage onSubscribe(@DestinationVariable("id") String id,
                                       Principal principal,
                                       StompHeaderAccessor headerAccessor) {
      String userId = principal.getName();
      String sessionId = headerAccessor.getSessionId();
      String subscriptionId = headerAccessor.getSubscriptionId();
      roomSubscriptionHelper.onSubscribe(userId, sessionId, subscriptionId, id);
      return RoomStompMessage.initialData(roomService.getActiveUsers(id), roomService.getEvents(id));
   }

   @MessageMapping("/room/{id}/message")
   @SendTo("/topic/room/{id}")
   public RoomStompMessage message(@DestinationVariable("id") String roomId,
                                   @Payload String message,
                                   Principal principal) {
      return RoomStompMessage.event(roomService.addMessage(roomId, principal.getName(), message));
   }

   @MessageMapping("/room/{id}/part")
   @SendToUser("/queue/room/{id}")
   public Map<String, String> part() {
      // Tell all clients for the current user to leave the given room
      return Map.of("type", "LEAVE");
   }

}
