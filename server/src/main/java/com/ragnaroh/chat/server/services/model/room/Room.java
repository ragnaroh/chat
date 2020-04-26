package com.ragnaroh.chat.server.services.model.room;

import static java.util.Objects.requireNonNull;

import java.util.List;

import com.ragnaroh.chat.server.services.model.room.event.Event;

public final class Room {

   private final String id;
   private final String name;
   private final List<User> users;
   private final List<Event> events;

   private Room(Builder builder) {
      this.id = requireNonNull(builder.id);
      this.name = requireNonNull(builder.name);
      this.users = requireNonNull(builder.users);
      this.events = requireNonNull(builder.events);
   }

   public String getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public List<User> getUsers() {
      return users;
   }

   public List<Event> getEvents() {
      return events;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {

      private String id;
      private String name;
      private List<User> users;
      private List<Event> events;

      private Builder() {}

      public Builder id(String id) {
         this.id = id;
         return this;
      }

      public Builder name(String name) {
         this.name = name;
         return this;
      }

      public Builder users(List<User> users) {
         this.users = users;
         return this;
      }

      public Builder events(List<Event> events) {
         this.events = events;
         return this;
      }

      public Room build() {
         return new Room(this);
      }
   }

   public static final class User {

      public enum Status {
         PENDING,
         ACTIVE,
         INACTIVE
      }

      private final String username;
      private final Status status;

      public User(String username, Status status) {
         this.username = requireNonNull(username);
         this.status = requireNonNull(status);
      }

      public String getUsername() {
         return username;
      }

      public Status getStatus() {
         return status;
      }

   }

}
