package com.ragnaroh.chat.server.services;

import static com.ragnaroh.chat.server.services.InputValidation.requireInputLength;
import static com.ragnaroh.chat.server.services.InputValidation.requireInputMatches;
import static com.ragnaroh.chat.server.services.InputValidation.requireInputNotNull;
import static com.ragnaroh.chat.server.services.InputValidation.requireInputTrimmed;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import com.ragnaroh.chat.server.common.Holder;
import com.ragnaroh.chat.server.common.exceptions.IllegalInputException;
import com.ragnaroh.chat.server.common.exceptions.NotFoundException;
import com.ragnaroh.chat.server.services.RoomServiceImpl.RoomUser.SubscriptionStatus;

@Service
public class RoomServiceImpl implements RoomService {

   private static final String VALID_NAME_REGEX = "[\\p{Alnum}_\\- ]+";

   private final Map<String, Room> rooms = new ConcurrentHashMap<>();

   @Override
   public String createRoom(String name) {
      requireInputTrimmed("name", name);
      requireInputLength("name", name, 1, 20);
      requireInputMatches("name", name, VALID_NAME_REGEX);
      return registerRoom(name).getId();
   }

   private Room registerRoom(String name) {
      var created = new AtomicBoolean();
      var id = RandomStringUtils.randomAlphanumeric(6);
      var room = rooms.computeIfAbsent(id, key -> {
         created.set(true);
         return new Room(key, name);
      });
      if (created.get()) {
         return room;
      }
      return registerRoom(name);
   }

   @Override
   public Room getRoom(String roomId) {
      Room room = rooms.get(roomId);
      if (room == null) {
         throwRoomNotFoundException(roomId);
      }
      return room;
   }

   @Override
   public List<String> getActiveUsers(String roomId) {
      return getRoom(roomId).getActiveUsers();
   }

   @Override
   public boolean addUser(String roomId, String userId, String username) {
      requireInputNotNull("roomId", roomId);
      requireInputNotNull("userId", userId);
      requireInputTrimmed("username", username);
      requireInputLength("username", username, 1, 16);
      requireInputMatches("username", username, VALID_NAME_REGEX);
      var holder = new Holder<>(true);
      Room room = rooms.computeIfPresent(roomId, (i, r) -> {
         if (r.users.values().stream().map(RoomUser::getName).anyMatch(username::equals)) {
            holder.set(false);
            return r;
         }
         if (r.users.containsKey(userId)) {
            throw new IllegalInputException("User with id {} is already in room with id {}", userId, roomId);
         }
         r.users.put(userId, new RoomUser(userId, username, SubscriptionStatus.PENDING));
         return r;
      });
      if (room == null) {
         throwRoomNotFoundException(roomId);
      }
      return holder.get();
   }

   @Override
   public boolean userExists(String roomId, String userId) {
      return getRoom(roomId).users.containsKey(userId);
   }

   @Override
   public String getUsername(String roomId, String userId) {
      RoomUser user = getRoom(roomId).users.get(userId);
      if (user == null) {
         return null;
      }
      return user.username;
   }

   @Override
   public SequencedRoomEvent activateUser(String roomId, String userId) {
      var holder = new Holder<SequencedRoomEvent>();
      Room room = rooms.computeIfPresent(roomId, (rid, r) -> {
         RoomUser user = r.users.get(userId);
         if (user == null) {
            throw new NotFoundException("User with id <{}> is not in room with id <{}>.", userId, roomId);
         }
         if (user.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            r.users.put(userId, new RoomUser(user.id, user.username, SubscriptionStatus.ACTIVE));
            UserEnter event = new UserEnter(LocalDateTime.now(), user.getName());
            holder.set(r.addEvent(event));
         }
         return r;
      });
      if (room == null) {
         throwRoomNotFoundException(roomId);
      }
      return holder.get();
   }

   @Override
   public SequencedRoomEvent removeUser(String roomId, String userId) {
      var holder = new Holder<SequencedRoomEvent>();
      Room room = rooms.computeIfPresent(roomId, (i, r) -> {
         RoomUser user = r.users.remove(userId);
         if (user == null) {
            throw new NotFoundException("User with id <{}> is not in room with id <{}>.", userId, roomId);
         }
         UserLeave event = new UserLeave(LocalDateTime.now(), user.getName());
         holder.set(r.addEvent(event));
         return r;
      });
      if (room == null) {
         throwRoomNotFoundException(roomId);
      }
      return holder.get();
   }

   private static void throwRoomNotFoundException(String roomId) {
      throw new NotFoundException("No room with id {}", roomId);
   }

   @Override
   public Map<String, SequencedRoomEvent> removeUserFromAllRooms(String userId) {
      var result = new HashMap<String, SequencedRoomEvent>();
      rooms.forEach((i, r) -> {
         RoomUser user = r.users.remove(userId);
         if (user != null && user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
            UserLeave event = new UserLeave(LocalDateTime.now(), user.getName());
            result.put(i, r.addEvent(event));
         }
      });
      return result;
   }

   @Override
   public SequencedRoomEvent addMessage(String roomId, String userId, String text) {
      var holder = new Holder<SequencedRoomEvent>();
      var room = rooms.computeIfPresent(roomId, (i, r) -> {
         RoomUser user = r.users.get(userId);
         var event = new Message(LocalDateTime.now(), user.getName(), text);
         holder.set(r.addEvent(event));
         return r;
      });
      if (room == null) {
         throwRoomNotFoundException(roomId);
      }
      return holder.get();
   }

   public static final class Room implements Serializable {

      private final String id;
      private final String name;
      private final Map<String, RoomUser> users = new HashMap<>();
      private final List<SequencedRoomEvent> events = new ArrayList<>();

      private final AtomicInteger eventSequenceNumber = new AtomicInteger(0);

      public Room(String id, String name) {
         this.id = requireNonNull(id);
         this.name = requireNonNull(name);
      }

      public String getId() {
         return id;
      }

      public String getName() {
         return name;
      }

      private synchronized SequencedRoomEvent addEvent(RoomEvent event) {
         SequencedRoomEvent sequencedEvent = new SequencedRoomEvent(eventSequenceNumber.getAndIncrement(), event);
         events.add(sequencedEvent);
         return sequencedEvent;
      }

      public List<String> getActiveUsers() {
         return unmodifiableList(users
               .values()
               .stream()
               .filter(user -> user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE)
               .map(RoomUser::getName)
               .collect(toList()));
      }

      public List<SequencedRoomEvent> getEvents() {
         return unmodifiableList(events);
      }

   }

   public static final class RoomUser implements Serializable {

      public enum SubscriptionStatus {
         PENDING,
         ACTIVE
      }

      private final String id;
      private final String username;
      private final SubscriptionStatus subscriptionStatus;

      private RoomUser(String id, String username, SubscriptionStatus subscriptionStatus) {
         this.id = requireNonNull(id);
         this.username = requireNonNull(username);
         this.subscriptionStatus = requireNonNull(subscriptionStatus);
      }

      public String getId() {
         return id;
      }

      public String getName() {
         return username;
      }

      public SubscriptionStatus getSubscriptionStatus() {
         return subscriptionStatus;
      }

   }

   public static final class SequencedRoomEvent implements Serializable {

      private final int sequenceNumber;
      private final RoomEvent event;

      private SequencedRoomEvent(int sequenceNumber, RoomEvent event) {
         this.sequenceNumber = sequenceNumber;
         this.event = event;
      }

      public int getSequenceNumber() {
         return sequenceNumber;
      }

      public RoomEvent getEvent() {
         return event;
      }

   }

   public abstract static class RoomEvent implements Serializable {

      public enum Type {
         MESSAGE,
         USER_ENTERS,
         USER_LEAVES
      }

      private final LocalDateTime time;

      private RoomEvent(LocalDateTime time) {
         this.time = requireNonNull(time);
      }

      public LocalDateTime getTime() {
         return time;
      }

      public abstract Type getType();
   }

   public abstract static class RoomUserEvent extends RoomEvent {

      private final String username;

      private RoomUserEvent(LocalDateTime time, String username) {
         super(time);
         this.username = requireNonNull(username);
      }

      public String getUsername() {
         return username;
      }

   }

   public static final class Message extends RoomUserEvent {

      private final String text;

      private Message(LocalDateTime time, String username, String text) {
         super(time, username);
         this.text = requireNonNull(text);
      }

      public String getText() {
         return text;
      }

      @Override
      public Type getType() {
         return Type.MESSAGE;
      }

   }

   public static final class UserEnter extends RoomUserEvent {

      private UserEnter(LocalDateTime time, String username) {
         super(time, username);
      }

      @Override
      public Type getType() {
         return Type.USER_ENTERS;
      }

   }

   public static final class UserLeave extends RoomUserEvent {

      private UserLeave(LocalDateTime time, String username) {
         super(time, username);
      }

      @Override
      public Type getType() {
         return Type.USER_LEAVES;
      }

   }
}
