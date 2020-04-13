package com.ragnaroh.chat.server.services;

import static java.lang.String.format;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

   @Around("@within(org.springframework.stereotype.Service)")
   public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
      long start = System.currentTimeMillis();
      Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
      logStart(logger, joinPoint);
      try {
         Object result = joinPoint.proceed();
         long duration = System.currentTimeMillis() - start;
         if (result == null) {
            logEnd(logger, duration, "no return value");
         } else {
            logEnd(logger, duration, "returning " + returnSummary(result));
         }
         return result;
      } catch (Throwable t) {
         long duration = System.currentTimeMillis() - start;
         logEnd(logger, duration, "throwing " + throwSummary(t));
         throw t;
      }
   }

   private static void logStart(Logger logger, ProceedingJoinPoint joinPoint) {
      if (logger.isInfoEnabled()) {
         logger
               .info("START: operation[{}] args[{}]",
                     joinPoint.getSignature().getName(),
                     argsToString(joinPoint.getArgs()));
      }
   }

   private static String argsToString(Object[] args) {
      return Stream.of(args).map(ServiceLoggingAspect::argToString).collect(Collectors.joining(","));
   }

   private static String argToString(Object arg) {
      if (arg == null) {
         return "<null>";
      }
      if (arg.getClass().isArray()) {
         return String.valueOf(List.of((Object[]) arg));
      }
      return String.valueOf(arg);
   }

   private static void logEnd(Logger logger, long duration, String summary) {
      logger.info("END: execution took {} ms, {}", duration, summary);
   }

   private String returnSummary(Object result) {
      Class<?> resultType = result.getClass();
      if (resultType.isArray()) {
         return format("Array of length <%d>", Array.getLength(result));
      }
      if (result instanceof Collection) {
         return format("Collection of size <%d>", ((Collection<?>) result).size());
      }
      if (result instanceof Map) {
         return format("Map of size <%d>", ((Map<?, ?>) result).size());
      }
      if (result instanceof Boolean || result instanceof Integer) {
         return format("<%s>", result);
      }
      return resultType.getSimpleName() + " instance";
   }

   private String throwSummary(Throwable throwable) {
      return format("%s with message %s", throwable.getClass().getSimpleName(), throwable.getMessage());
   }

}
