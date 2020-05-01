package com.ragnaroh.chat.server.services;

import static com.ragnaroh.chat.server.services.InputValidation.requireInputLength;
import static com.ragnaroh.chat.server.services.InputValidation.requireInputMatches;
import static com.ragnaroh.chat.server.services.InputValidation.requireInputNotNull;
import static com.ragnaroh.chat.server.services.InputValidation.requireInputTrimmed;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ragnaroh.chat.server.common.exceptions.IllegalInputException;
import com.ragnaroh.chat.server.common.exceptions.NotFoundException;
import com.ragnaroh.chat.server.services.model.room.Room;
import com.ragnaroh.chat.server.services.model.room.RoomLite;
import com.ragnaroh.chat.server.services.model.room.event.Event;

@Service
public class RoomServiceImpl implements RoomService {

   private static final String VALID_NAME_REGEX = "[\\p{Alnum}_\\- ]+";

   private final SynchronizedExecutor<String> synchronizedExecutor = new SynchronizedExecutor<>();

   @Autowired
   private RoomDao roomDao;
   @Autowired
   private TransactionTemplate transactionTemplate;
   @Autowired
   private UserDao userDao;

   @Override
   public List<RoomLite> getRoomsLite() {
      return roomDao.fetchRoomsLite();
   }

   @Override
   public String createRoom(String name) {
      requireInputTrimmed("name", name);
      requireInputLength("name", name, 1, 20);
      requireInputMatches("name", name, VALID_NAME_REGEX);
      return registerRoom(name);
   }

   private String registerRoom(String name) {
      var id = RandomStringUtils.randomAlphanumeric(6);
      if (!roomDao.roomExists(id)) {
         roomDao.insertRoom(id, name);
         return id;
      }
      return registerRoom(name);
   }

   @Override
   public Room getRoom(String roomId) {
      Room room = roomDao.fetchRoom(roomId);
      if (room == null) {
         throw new NotFoundException("No room with id {}", roomId);
      }
      return room;
   }

   @Override
   public List<String> getActiveUsers(String roomId) {
      return roomDao.fetchActiveUsers(roomId);
   }

   @Override
   public List<Event> getEvents(String roomId) {
      return roomDao.fetchEvents(roomId);
   }

   @Override
   public boolean addUser(String roomId, String userId, String username) {
      requireInputNotNull("roomId", roomId);
      requireInputNotNull("userId", userId);
      requireInputTrimmed("username", username);
      requireInputLength("username", username, 1, 16);
      requireInputMatches("username", username, VALID_NAME_REGEX);
      int userKey = getUserKey(userId);
      return synchronizedExecutor.execute(roomId, () -> {
         Room.User user = roomDao.fetchUser(roomId, userId);
         if (user != null
               && (user.getStatus() == Room.User.Status.PENDING || user.getStatus() == Room.User.Status.ACTIVE)) {
            throw new IllegalInputException("User <{}> is already in room with ID <{}>", userKey, roomId);
         }
         if (!roomDao.usernameIsAvailable(roomId, username)) {
            return false;
         }
         if (user == null) {
            roomDao.insertPendingUser(roomId, userKey, username);
         } else {
            roomDao.updateUsernameAndStatus(roomId, userKey, username, Room.User.Status.PENDING);
         }
         return true;
      });
   }

   private int getUserKey(String userId) {
      Integer key = userDao.getKeyOrNull(userId);
      if (key == null) {
         return userDao.insertUser(userId);
      }
      return key;
   }

   @Override
   public boolean readdUser(String roomId, String userId) {
      requireInputNotNull("roomId", roomId);
      requireInputNotNull("userId", userId);
      return synchronizedExecutor.execute(roomId, () -> {
         Room.User user = roomDao.fetchUser(roomId, userId);
         if (user == null) {
            return false;
         }
         if (user.getStatus() == Room.User.Status.INACTIVE) {
            if (roomDao.usernameIsAvailable(roomId, user.getUsername())) {
               roomDao.updateUserStatus(roomId, userId, Room.User.Status.PENDING);
               return true;
            }
            return false;
         }
         return true;
      });
   }

   @Override
   public Event activateUser(String roomId, String userId) {
      return synchronizedExecutor.execute(roomId, () -> {
         Room.User user = roomDao.fetchUser(roomId, userId);
         if (user == null) {
            throw new NotFoundException("User with ID <{}> is not in room with ID <{}>.", userId, roomId);
         }
         if (user.getStatus() == Room.User.Status.PENDING) {
            return tx(() -> {
               roomDao.updateUserStatus(roomId, userId, Room.User.Status.ACTIVE);
               int sequenceNumber = getNextSequenceNumber(roomId);
               roomDao.insertJoinedEvent(roomId, sequenceNumber, userId, LocalDateTime.now());
               return roomDao.fetchEvent(roomId, sequenceNumber);
            });
         }
         return null;
      });
   }

   @Override
   public Event deactivateUser(String roomId, String userId) {
      return synchronizedExecutor.execute(roomId, () -> {
         Room.User user = roomDao.fetchUser(roomId, userId);
         if (user == null) {
            throw new NotFoundException("User with ID <{}> is not in room with ID <{}>.", userId, roomId);
         }
         if (user.getStatus() == Room.User.Status.ACTIVE) {
            return tx(() -> {
               roomDao.updateUserStatus(roomId, userId, Room.User.Status.INACTIVE);
               int sequenceNumber = getNextSequenceNumber(roomId);
               roomDao.insertPartedEvent(roomId, sequenceNumber, userId, LocalDateTime.now());
               return roomDao.fetchEvent(roomId, sequenceNumber);
            });
         }
         return null;
      });
   }

   @Override
   public Event addMessage(String roomId, String userId, String text) {
      return synchronizedExecutor.execute(roomId, () -> {
         int sequenceNumber = getNextSequenceNumber(roomId);
         roomDao.insertMessageEvent(roomId, sequenceNumber, userId, text, LocalDateTime.now());
         return roomDao.fetchEvent(roomId, sequenceNumber);
      });
   }

   private int getNextSequenceNumber(String roomId) {
      return defaultIfNull(roomDao.fetchMaxEventSequenceNumber(roomId), 0) + 1;
   }

   private <T> T tx(Supplier<T> supplier) {
      return transactionTemplate.execute(status -> supplier.get());
   }

}
