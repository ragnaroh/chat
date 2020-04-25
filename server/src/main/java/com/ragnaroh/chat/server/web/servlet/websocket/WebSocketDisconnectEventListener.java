package com.ragnaroh.chat.server.web.servlet.websocket;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.ragnaroh.chat.server.services.RoomService;
import com.ragnaroh.chat.server.services.RoomServiceImpl.SequencedRoomEvent;

@Component
public class WebSocketDisconnectEventListener implements ApplicationListener<SessionDisconnectEvent> {

   @Autowired
   private RoomService roomService;
   @Autowired
   private StompTemplate stompTemplate;

   @Override
   public void onApplicationEvent(SessionDisconnectEvent event) {
      StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
      Map<String, SequencedRoomEvent> events = roomService.removeUserFromAllRooms(headerAccessor.getUser().getName());
      for (var entry : events.entrySet()) {
         String roomId = entry.getKey();
         stompTemplate.sendToRoom(roomId, RoomStompMessage.event(entry.getValue()));
         stompTemplate.sendToRoom(roomId, RoomStompMessage.users(roomService.getActiveUsers(roomId)));
      }
   }

}
