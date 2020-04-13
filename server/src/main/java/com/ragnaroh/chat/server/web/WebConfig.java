package com.ragnaroh.chat.server.web;

import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.ragnaroh.chat.server.web.servlet.api.ApiServletConfig;
import com.ragnaroh.chat.server.web.servlet.app.AppServletConfig;
import com.ragnaroh.chat.server.web.servlet.websocket.WebSocketServletConfig;

@Configuration
@Import({ ServletWebServerFactoryAutoConfiguration.class, WebSocketServletAutoConfiguration.class })
public class WebConfig {

   @Bean
   public RequestLoggingFilter requestLoggingFilter() {
      return new RequestLoggingFilter();
   }

   @Bean
   public ServletRegistrationBean<DispatcherServlet> apiServletRegistration() {
      return servletRegistration("api", ApiServletConfig.class, 0, "/api/*");
   }

   @Bean
   public ServletRegistrationBean<DispatcherServlet> webSocketServletRegistration() {
      return servletRegistration("websocket", WebSocketServletConfig.class, 1, "/ws/*");
   }

   @Bean
   public ServletRegistrationBean<DispatcherServlet> appServletRegistration() {
      return servletRegistration("app", AppServletConfig.class, 2, "/chat/*");
   }

   private static ServletRegistrationBean<DispatcherServlet> servletRegistration(String name,
                                                                                 Class<?> configClass,
                                                                                 int loadOnStartup,
                                                                                 String... urlMappings) {
      var servlet = new DispatcherServlet();
      servlet.setContextClass(AnnotationConfigWebApplicationContext.class);
      servlet.setContextConfigLocation(configClass.getCanonicalName());
      var servletRegistrationBean = new ServletRegistrationBean<>(servlet, urlMappings);
      servletRegistrationBean.setName(name);
      servletRegistrationBean.setLoadOnStartup(loadOnStartup);
      return servletRegistrationBean;
   }

}
