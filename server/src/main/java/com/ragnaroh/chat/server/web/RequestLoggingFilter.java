package com.ragnaroh.chat.server.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLoggingFilter extends OncePerRequestFilter {

   @SuppressWarnings({ "hiding", "java:S2387" })
   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
         throws ServletException, IOException {
      long start = System.currentTimeMillis();
      logger.info("{} {}", request.getMethod(), request.getRequestURI());
      try {
         filterChain.doFilter(request, response);
      } finally {
         long duration = System.currentTimeMillis() - start;
         logger
               .info("{} {} took {} ms, returned status code {}",
                     request.getMethod(),
                     request.getRequestURI(),
                     duration,
                     response.getStatus());
      }
   }

}
