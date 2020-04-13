package com.ragnaroh.chat.server.services;

import com.ragnaroh.chat.server.common.exceptions.IllegalInputException;

public final class InputValidation {

   private InputValidation() {}

   public static <T> T requireInputNotNull(String name, T value) {
      if (value == null) {
         throw new IllegalInputException("Value of <{}> was null", name);
      }
      return value;
   }

   public static String requireInputTrimmed(String name, String value) {
      requireInputNotNull(name, value);
      if (!value.equals(value.trim())) {
         throw new IllegalInputException("Value of <{}> was not trimmed", name);
      }
      return value;
   }

   public static String requireInputLength(String name, String value, int minLength, int maxLength) {
      requireInputNotNull(name, value);
      var length = value.length();
      if (length < minLength || length > maxLength) {
         throw new IllegalInputException("Value of <{}> had length {}, expected between {} and {}",
                                         name,
                                         length,
                                         minLength,
                                         maxLength);
      }
      return value;
   }

   public static String requireInputMatches(String name, String value, String regex) {
      requireInputNotNull(name, value);
      if (!value.matches(regex)) {
         throw new IllegalInputException("Value of <{}> did not match regex <{}>", name, regex);
      }
      return value;
   }

}
