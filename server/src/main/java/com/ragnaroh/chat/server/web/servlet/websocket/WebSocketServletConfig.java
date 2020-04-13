package com.ragnaroh.chat.server.web.servlet.websocket;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@ComponentScan
@EnableWebSocketMessageBroker
public class WebSocketServletConfig implements WebSocketMessageBrokerConfigurer {

   @Override
   public void registerStompEndpoints(StompEndpointRegistry registry) {
      registry
            .addEndpoint("/stomp")
            .setHandshakeHandler(new StompHandshakeHandler())
            .withSockJS()
            .setClientLibraryUrl("../../resources/sockjs.min.js");
   }

   @Override
   public void configureMessageBroker(MessageBrokerRegistry registry) {
      registry.enableSimpleBroker("/topic");
      registry.setApplicationDestinationPrefixes("/app", "/topic");
   }

}
