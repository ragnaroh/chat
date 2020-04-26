package com.ragnaroh.chat.server.services.model.room.event;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;

public abstract class Event {

   public enum Type {
      MESSAGE,
      JOINED,
      PARTED
   }

   private final int sequenceNumber;
   private final String username;
   private final LocalDateTime timestamp;

   private Event(Builder<?, ?> builder) {
      this.sequenceNumber = requireNonNull(builder.sequenceNumber);
      this.username = requireNonNull(builder.username);
      this.timestamp = requireNonNull(builder.timestamp);
   }

   public int getSequenceNumber() {
      return sequenceNumber;
   }

   public String getUsername() {
      return username;
   }

   public LocalDateTime getTimestamp() {
      return timestamp;
   }

   public abstract Type getType();

   public abstract static class Builder<B extends Builder<B, E>, E extends Event> {

      private Integer sequenceNumber;
      private String username;
      private LocalDateTime timestamp;

      private Builder() {}

      public B sequenceNumber(Integer sequenceNumber) {
         this.sequenceNumber = sequenceNumber;
         return (B) this;
      }

      public B username(String username) {
         this.username = username;
         return (B) this;
      }

      public B timestamp(LocalDateTime timestamp) {
         this.timestamp = timestamp;
         return (B) this;
      }

      public abstract E build();

   }

   public static final class Joined extends Event {

      private Joined(Builder builder) {
         super(builder);
      }

      @Override
      public Type getType() {
         return Type.JOINED;
      }

      public static Builder builder() {
         return new Builder();
      }

      public static final class Builder extends Event.Builder<Builder, Joined> {

         private Builder() {}

         @Override
         public Joined build() {
            return new Joined(this);
         }
      }

   }

   public static final class Parted extends Event {

      private Parted(Builder builder) {
         super(builder);
      }

      @Override
      public Type getType() {
         return Type.PARTED;
      }

      public static Builder builder() {
         return new Builder();
      }

      public static final class Builder extends Event.Builder<Builder, Parted> {

         private Builder() {}

         @Override
         public Parted build() {
            return new Parted(this);
         }
      }

   }

   public static final class Message extends Event {

      private final String text;

      private Message(Builder builder) {
         super(builder);
         this.text = requireNonNull(builder.text);
      }

      @Override
      public Type getType() {
         return Type.MESSAGE;
      }

      public String getText() {
         return text;
      }

      public static Builder builder() {
         return new Builder();
      }

      public static final class Builder extends Event.Builder<Builder, Message> {

         private String text;

         public Builder text(String text) {
            this.text = text;
            return this;
         }

         private Builder() {}

         @Override
         public Message build() {
            return new Message(this);
         }
      }

   }

}
