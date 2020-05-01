package com.ragnaroh.chat.server.services.model.room;

import static java.util.Objects.requireNonNull;

public class RoomLite {

   private final String id;
   private final String name;
   private final int users;

   private RoomLite(Builder builder) {
      this.id = requireNonNull(builder.id);
      this.name = requireNonNull(builder.name);
      this.users = requireNonNull(builder.users);
   }

   public String getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public int getUsers() {
      return users;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {

      private String id;
      private String name;
      private Integer users;

      private Builder() {}

      public Builder id(String id) {
         this.id = id;
         return this;
      }

      public Builder name(String name) {
         this.name = name;
         return this;
      }

      public Builder users(Integer users) {
         this.users = users;
         return this;
      }

      public RoomLite build() {
         return new RoomLite(this);
      }
   }

}
