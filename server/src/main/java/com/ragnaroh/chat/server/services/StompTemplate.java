package com.ragnaroh.chat.server.services;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompTemplate implements ApplicationListener<ContextRefreshedEvent> {

   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   private SimpMessagingTemplate messagingTemplate;

   @Override
   public void onApplicationEvent(ContextRefreshedEvent event) {
      if (this.messagingTemplate == null) {
         // SimpMessagingTemplate bean is defined in another context, so it cannot be injected directly.
         try {
            var applicationContext = event.getApplicationContext();
            this.messagingTemplate = applicationContext.getBean(SimpMessagingTemplate.class);
         } catch (NoSuchBeanDefinitionException e) {
            // Ignored
         }
      }
   }

   public void send(String destination, Object message) {
      logger.debug("Sending message of type {} to {}", ClassUtils.getSimpleName(message), destination);
      messagingTemplate.convertAndSend(destination, message);
   }
}
