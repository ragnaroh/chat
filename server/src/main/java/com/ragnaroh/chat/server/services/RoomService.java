package com.ragnaroh.chat.server.services;

import java.util.List;
import java.util.Map;

import com.ragnaroh.chat.server.services.RoomServiceImpl.Room;
import com.ragnaroh.chat.server.services.RoomServiceImpl.SequencedRoomEvent;

public interface RoomService {

   public String createRoom(String name);

   public Room getRoom(String roomId);

   public List<String> getActiveUsers(String roomId);

   public boolean addUser(String roomId, String userId, String username);

   public boolean userExists(String roomId, String userId);

   public String getUsername(String roomId, String userId);

   public SequencedRoomEvent activateUser(String roomId, String userId);

   public SequencedRoomEvent removeUser(String roomId, String userId);

   public Map<String, SequencedRoomEvent> removeUserFromAllRooms(String userId);

   public SequencedRoomEvent addMessage(String roomId, String userId, String text);

}
