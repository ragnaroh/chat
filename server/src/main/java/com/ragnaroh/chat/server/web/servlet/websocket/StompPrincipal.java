package com.ragnaroh.chat.server.web.servlet.websocket;

import static java.util.Objects.requireNonNull;

import java.security.Principal;

public class StompPrincipal implements Principal {

   private final String name;

   StompPrincipal(String name) {
      this.name = requireNonNull(name);
   }

   @Override
   public String getName() {
      return name;
   }

}
