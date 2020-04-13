package com.ragnaroh.chat.server.services;

import com.ragnaroh.chat.server.services.RoomServiceImpl.Room;
import com.ragnaroh.chat.server.services.RoomServiceImpl.RoomUpdate;

public interface RoomService {

   public String createRoom(String name);

   public Room getRoom(String roomId);

   public boolean addUser(String roomId, String userId, String username);

   public boolean userExists(String roomId, String userId);

   public String getRoomUsername(String roomId, String userId);

   public RoomUpdate updateSubscriptionStatusToActive(String roomId, String userId);

   public void removeUser(String roomId, String userId);

   public void removeUserFromAllRooms(String userId);

   public RoomUpdate addMessage(String roomId, String userId, String text);

}
