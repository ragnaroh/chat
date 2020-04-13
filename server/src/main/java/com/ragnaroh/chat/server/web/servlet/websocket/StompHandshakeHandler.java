package com.ragnaroh.chat.server.web.servlet.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class StompHandshakeHandler extends DefaultHandshakeHandler {

   @Override
   protected Principal determineUser(ServerHttpRequest request,
                                     WebSocketHandler wsHandler,
                                     Map<String, Object> attributes) {
      return new StompPrincipal(((ServletServerHttpRequest) request).getServletRequest().getSession().getId());
   }

}
