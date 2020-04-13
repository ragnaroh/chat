package com.ragnaroh.chat.server.web.servlet.app;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.AbstractController;

@Configuration
@EnableWebMvc
public class AppServletConfig implements WebMvcConfigurer {

   @Override
   public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/index.html").addResourceLocations("classpath:/public/");
      registry.addResourceHandler("/resources/**").addResourceLocations("classpath:/public/resources/");
   }

   @Override
   public void addViewControllers(ViewControllerRegistry registry) {
      registry.addViewController("/").setViewName("forward:/chat/index.html");
   }

   @Bean
   public SimpleUrlHandlerMapping fallbackHandlerMapping() {
      var handlerMapping = new SimpleUrlHandlerMapping();
      var properties = new Properties(1);
      properties.put("/**", "fallbackController");
      handlerMapping.setMappings(properties);
      handlerMapping.setOrder(Ordered.LOWEST_PRECEDENCE);
      return handlerMapping;
   }

   @Bean
   public FallbackController fallbackController() {
      return new FallbackController();
   }

   public static final class FallbackController extends AbstractController {

      @Override
      protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
         return new ModelAndView("forward:/chat/");
      }

   }

}
