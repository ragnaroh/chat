package com.ragnaroh.chat.server.app;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

import com.ragnaroh.chat.server.services.ServicesConfig;
import com.ragnaroh.chat.server.web.WebConfig;

@Import({ ServicesConfig.class, WebConfig.class })
public class Application {

   public static void main(String[] args) {
      SpringApplication.run(Application.class, args);
   }

}
