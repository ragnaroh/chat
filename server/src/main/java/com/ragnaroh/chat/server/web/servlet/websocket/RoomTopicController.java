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
import com.ragnaroh.chat.server.services.RoomServiceImpl.RoomUpdate;

@Controller
public class RoomTopicController {

   @Autowired
   private RoomService roomService;
   @Autowired
   private RoomSubscriptionHelper roomSubscriptionHelper;

   @SubscribeMapping("/room/{id}")
   public RoomUpdate onSubscribe(@DestinationVariable("id") String id,
                                 @Header("id") String subscriptionId,
                                 Principal principal) {
      roomSubscriptionHelper.onSubscribe(subscriptionId, principal.getName(), id);
      return roomService.updateSubscriptionStatusToActive(id, principal.getName());
   }

   @MessageMapping("/room/{id}/message")
   @SendTo("/topic/room/{id}")
   public RoomUpdate message(@DestinationVariable("id") String roomId, @Payload String message, Principal principal) {
      return roomService.addMessage(roomId, principal.getName(), message);
   }

   public static final class InMessage {

      private String text;
      private String user;

      public String getText() {
         return text;
      }

      public void setText(String text) {
         this.text = text;
      }

      public String getUser() {
         return user;
      }

      public void setUser(String user) {
         this.user = user;
      }

   }

}
