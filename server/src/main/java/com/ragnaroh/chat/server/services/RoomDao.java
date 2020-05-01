package com.ragnaroh.chat.server.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.ragnaroh.chat.server.services.model.room.Room;
import com.ragnaroh.chat.server.services.model.room.Room.User.Status;
import com.ragnaroh.chat.server.services.model.room.RoomLite;
import com.ragnaroh.chat.server.services.model.room.event.Event;

@Repository
public class RoomDao extends Dao {

   @Autowired
   private UserDao userDao;

   List<RoomLite> fetchRoomsLite() {
      return query("""
            SELECT r.ID, r.NAME, COUNT(*) AS USERS FROM Room r, RoomUser ru
            WHERE r.KEY = ru.ROOM_KEY
              AND ru.STATUS = :active
            GROUP BY r.ID, r.NAME
            """,
                   Map.of("active", Room.User.Status.ACTIVE.name()),
                   rs -> RoomLite
                         .builder()
                         .id(rs.getString("ID"))
                         .name(rs.getString("NAME"))
                         .users(rs.getInt("USERS"))
                         .build());
   }

   int insertRoom(String id, String name) {
      return updateAndReturnId("""
            INSERT INTO Room (`ID`,`NAME`) VALUES (:id, :name)
            """, Map.of("id", id, "name", name));
   }

   boolean roomExists(String id) {
      return queryForInteger("""
            SELECT COUNT(*) FROM Room WHERE ID = :id
            """, Map.of("id", id)) > 0;
   }

   Room fetchRoom(String id) {
      return queryForSingleResultOrNull("""
            SELECT * FROM Room WHERE ID = :id
            """, Map.of("id", id), this::extractRoom);
   }

   List<String> fetchActiveUsers(String roomId) {
      return queryForStringList("""
            SELECT USERNAME FROM RoomUser
            WHERE ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
              AND STATUS = :status
            """, Map.of("roomId", roomId, "status", Room.User.Status.ACTIVE.name()));
   }

   List<Event> fetchEvents(String roomId) {
      return fetchEvents(fetchRoomKey(roomId));
   }

   boolean usernameIsAvailable(String roomId, String username) {
      return queryForInteger("""
            SELECT COUNT(*) FROM RoomUser
            WHERE ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
              AND USERNAME = :username
              AND STATUS IN (:pending,:active)
            """,
                             paramsBuilder()
                                   .put("roomId", roomId)
                                   .put("username", username)
                                   .put("pending", Room.User.Status.PENDING.name())
                                   .put("active", Room.User.Status.ACTIVE.name())
                                   .toMap()) == 0;
   }

   void insertPendingUser(String roomId, int userKey, String username) {
      update("""
            INSERT INTO RoomUser (`ROOM_KEY`,`USER_KEY`,`USERNAME`,`STATUS`)
            VALUES (SELECT KEY FROM Room WHERE ID = :roomId,:userKey,:username,:status)
            """,
             paramsBuilder()
                   .put("roomId", roomId)
                   .put("userKey", userKey)
                   .put("username", username)
                   .put("status", Room.User.Status.PENDING.name())
                   .toMap());
   }

   void updateUsernameAndStatus(String roomId, int userKey, String username, Room.User.Status status) {
      update("""
            UPDATE RoomUser
            SET STATUS = :status
              , USERNAME = :username
            WHERE ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
              AND USER_KEY = :userKey
            """,
             paramsBuilder()
                   .put("roomId", roomId)
                   .put("userKey", userKey)
                   .put("username", username)
                   .put("status", status.name())
                   .toMap());
   }

   Room.User fetchUser(String roomId, String userId) {
      return queryForSingleResultOrNull("""
            SELECT USERNAME, STATUS FROM RoomUser
            WHERE ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
              AND USER_KEY = (SELECT KEY FROM User WHERE ID = :userId)
            """, Map.of("roomId", roomId, "userId", userId), this::extractUser);
   }

   void updateUserStatus(String roomId, String userId, Status status) {
      update("""
            UPDATE RoomUser
            SET STATUS = :status
            WHERE ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
              AND USER_KEY = (SELECT KEY FROM User WHERE ID = :userId)
            """, Map.of("roomId", roomId, "userId", userId, "status", status.name()));
   }

   Integer fetchMaxEventSequenceNumber(String roomId) {
      return queryForNullableInteger("""
            SELECT MAX(SEQUENCE_NUMBER) FROM RoomEvent
            WHERE ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
            """, Map.of("roomId", roomId));
   }

   void insertJoinedEvent(String roomId, int sequenceNumber, String userId, LocalDateTime timestamp) {
      insertEvent(fetchRoomKey(roomId), sequenceNumber, userDao.getKey(userId), timestamp, "JOINED");
   }

   void insertPartedEvent(String roomId, int sequenceNumber, String userId, LocalDateTime timestamp) {
      insertEvent(fetchRoomKey(roomId), sequenceNumber, userDao.getKey(userId), timestamp, "PARTED");
   }

   void insertMessageEvent(String roomId, int sequenceNumber, String userId, String text, LocalDateTime timestamp) {
      int roomKey = fetchRoomKey(roomId);
      int userKey = userDao.getKey(userId);
      insertEvent(roomKey, sequenceNumber, userKey, timestamp, "MESSAGE");
      update("""
            INSERT INTO RoomMessageEvent (`ROOM_KEY`,`SEQUENCE_NUMBER`,`TEXT`)
            VALUES (:roomKey, :sequenceNumber, :text)
            """, Map.of("roomKey", roomKey, "sequenceNumber", sequenceNumber, "text", text));
   }

   Event fetchEvent(String roomId, int sequenceNumber) {
      return queryForSingleResult("""
            SELECT re.*, ru.USERNAME, rme.TEXT FROM RoomEvent re
            INNER JOIN RoomUser ru
               ON ru.ROOM_KEY = re.ROOM_KEY
              AND ru.USER_KEY = re.USER_KEY
            LEFT JOIN RoomMessageEvent rme
               ON re.TYPE = 'MESSAGE'
              AND re.ROOM_KEY = rme.ROOM_KEY
              AND re.SEQUENCE_NUMBER = rme.SEQUENCE_NUMBER
            WHERE re.ROOM_KEY = (SELECT KEY FROM Room WHERE ID = :roomId)
              AND re.SEQUENCE_NUMBER = :sequenceNumber
            """, Map.of("roomId", roomId, "sequenceNumber", sequenceNumber), this::extractEvent);
   }

   private void insertEvent(int roomKey, int sequenceNumber, int userKey, LocalDateTime timestamp, String eventType) {
      update("""
            INSERT INTO RoomEvent (`ROOM_KEY`,`SEQUENCE_NUMBER`,`USER_KEY`,`TYPE`,`TIMESTAMP`)
            VALUES (:roomKey, :sequenceNumber, :userKey, :eventType, :timestamp)
            """,
             paramsBuilder()
                   .put("roomKey", roomKey)
                   .put("sequenceNumber", sequenceNumber)
                   .put("userKey", userKey)
                   .put("eventType", eventType)
                   .put("timestamp", timestamp)
                   .toMap());
   }

   private Room extractRoom(ResultSet rs) throws SQLException {
      int key = rs.getInt("KEY");
      return Room
            .builder()
            .id(rs.getString("ID"))
            .name(rs.getString("NAME"))
            .users(fetchUsers(key))
            .events(fetchEvents(key))
            .build();
   }

   private int fetchRoomKey(String roomId) {
      return queryForInteger("""
            SELECT KEY FROM Room WHERE ID = :roomId
            """, Map.of("roomId", roomId));
   }

   private List<Room.User> fetchUsers(int roomKey) {
      return query("""
            SELECT * FROM RoomUser WHERE ROOM_KEY = :roomKey
            """, Map.of("roomKey", roomKey), this::extractUser);
   }

   private Room.User extractUser(ResultSet rs) throws SQLException {
      return new Room.User(rs.getString("USERNAME"), Room.User.Status.valueOf(rs.getString("STATUS")));
   }

   private List<Event> fetchEvents(int roomKey) {
      return query("""
            SELECT re.*, ru.USERNAME, rme.TEXT FROM RoomEvent re
            INNER JOIN RoomUser ru
               ON ru.ROOM_KEY = re.ROOM_KEY
              AND ru.USER_KEY = re.USER_KEY
            LEFT JOIN RoomMessageEvent rme
               ON re.TYPE = 'MESSAGE'
              AND re.ROOM_KEY = rme.ROOM_KEY
              AND re.SEQUENCE_NUMBER = rme.SEQUENCE_NUMBER
            WHERE re.ROOM_KEY = :roomKey
            """, Map.of("roomKey", roomKey), this::extractEvent);
   }

   private Event extractEvent(ResultSet rs) throws SQLException {
      String type = rs.getString("TYPE");
      var builder = switch (type) {
      case "MESSAGE" -> Event.Message.builder().text(rs.getString("TEXT"));
      case "JOINED" -> Event.Joined.builder();
      case "PARTED" -> Event.Parted.builder();
      default -> throw new IllegalStateException("Illegal type: " + type);
      };
      return builder
            .sequenceNumber(rs.getInt("SEQUENCE_NUMBER"))
            .username(rs.getString("USERNAME"))
            .timestamp(rs.getObject("TIMESTAMP", LocalDateTime.class))
            .build();
   }

}
