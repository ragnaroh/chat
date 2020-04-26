package com.ragnaroh.chat.server.services;

import java.util.List;

import com.ragnaroh.chat.server.services.model.room.Room;
import com.ragnaroh.chat.server.services.model.room.event.Event;

public interface RoomService {

   public String createRoom(String name);

   public Room getRoom(String roomId);

   public List<String> getActiveUsers(String roomId);

   public List<Event> getEvents(String roomId);

   public boolean addUser(String roomId, String userId, String username);

   public boolean readdUser(String roomId, String userId);

   public Event activateUser(String roomId, String userId);

   public Event deactivateUser(String roomId, String userId);

   public Event addMessage(String roomId, String userId, String text);

}
