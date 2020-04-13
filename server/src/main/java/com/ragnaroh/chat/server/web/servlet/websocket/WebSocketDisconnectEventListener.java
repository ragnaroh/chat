package com.ragnaroh.chat.server.web.servlet.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.ragnaroh.chat.server.services.RoomService;

@Component
public class WebSocketDisconnectEventListener implements ApplicationListener<SessionDisconnectEvent> {

   @Autowired
   private RoomService roomService;

   @Override
   public void onApplicationEvent(SessionDisconnectEvent event) {
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
      roomService.removeUserFromAllRooms(headerAccessor.getUser().getName());
   }

}
