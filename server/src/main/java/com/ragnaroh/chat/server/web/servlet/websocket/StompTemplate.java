package com.ragnaroh.chat.server.web.servlet.websocket;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompTemplate {

   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   @Autowired
   private SimpMessagingTemplate messagingTemplate;

   public void sendToRoom(String roomId, RoomStompMessage message) {
      send("/topic/room/" + roomId, message);
   }

   public void send(String destination, Object message) {
      if (logger.isDebugEnabled()) {
         logger.debug("Sending message of type {} to {}", ClassUtils.getSimpleName(message), destination);
      }
      messagingTemplate.convertAndSend(destination, message);
   }

}
