package com.ragnaroh.chat.server.web.servlet.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebSocketUnsubscribeListener implements ApplicationListener<SessionUnsubscribeEvent> {

   @Autowired
   private RoomSubscriptionHelper subscriptionHelper;

   @Override
   public void onApplicationEvent(SessionUnsubscribeEvent event) {
      var headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
      String subscriptionId = headerAccessor.getFirstNativeHeader("id");
      subscriptionHelper.onUnsubscribe(subscriptionId);
   }

}
