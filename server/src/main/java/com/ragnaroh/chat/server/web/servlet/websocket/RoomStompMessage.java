package com.ragnaroh.chat.server.web.servlet.websocket;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ragnaroh.chat.server.services.RoomServiceImpl.SequencedRoomEvent;

public final class RoomStompMessage {

   public enum Type {
      INITIAL_DATA,
      EVENT,
      USERS;
   }

   private final Type type;
   private final Object object;

   private RoomStompMessage(Type type, Object object) {
      this.type = requireNonNull(type);
      this.object = object;
   }

   public static RoomStompMessage event(SequencedRoomEvent event) {
      return new RoomStompMessage(Type.EVENT, requireNonNull(event));
   }

   public static RoomStompMessage users(List<String> users) {
      return new RoomStompMessage(Type.USERS, sorted(requireNonNull(users)));
   }

   public static RoomStompMessage initialData(List<String> users, List<SequencedRoomEvent> events) {
      return new RoomStompMessage(Type.INITIAL_DATA, Map.of("users", users, "events", events));
   }

   public Type getType() {
      return type;
   }

   public Object getObject() {
      return object;
   }

   private static <T> List<T> sorted(Collection<T> collection) {
      return collection.stream().sorted().collect(toList());
   }

}
