package com.ragnaroh.chat.server.web.servlet.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketDisconnectEventListener implements ApplicationListener<SessionDisconnectEvent> {

   @Autowired
   private RoomSubscriptionHelper roomSubscriptionHelper;

   @Override
   public void onApplicationEvent(SessionDisconnectEvent event) {
      roomSubscriptionHelper.onDisconnect(event.getUser().getName(), event.getSessionId());
   }

}
