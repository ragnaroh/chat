package com.ragnaroh.chat.server.web.servlet.websocket;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ragnaroh.chat.server.common.Holder;
import com.ragnaroh.chat.server.services.RoomService;

@Component
public class RoomSubscriptionHelper {

   private final Map<String, UserSubscriptions> subscriptionsPerUser = new ConcurrentHashMap<>();

   @Autowired
   private RoomService roomService;
   @Autowired
   private StompTemplate stompTemplate;

   public void onSubscribe(String userId, String sessionId, String subscriptionId, String roomId) {
      subscriptionsPerUser.compute(userId, (uid, subs) -> {
         var nonNullSubs = subs == null ? new UserSubscriptions() : subs;
         if (!nonNullSubs.subscrptionExistsForRoom(roomId)) {
            activateUser(userId, roomId);
         }
         nonNullSubs.add(sessionId, subscriptionId, roomId);
         return nonNullSubs;
      });
   }

   private void activateUser(String userId, String roomId) {
      var event = roomService.activateUser(roomId, userId);
      if (event != null) {
         stompTemplate.sendToRoom(roomId, RoomStompMessage.event(event));
         stompTemplate.sendToRoom(roomId, RoomStompMessage.users(roomService.getActiveUsers(roomId)));
      }
   }

   public void onUnsubscribe(String userId, String sessionId, String subscriptionId) {
      subscriptionsPerUser.compute(userId, (uid, subs) -> {
         if (subs == null) {
            return null;
         }
         String roomId = subs.remove(sessionId, subscriptionId);
         if (roomId != null && !subs.subscrptionExistsForRoom(roomId)) {
            deactivateUser(roomId, userId);
         }
         return subs.isEmpty() ? null : subs;
      });
   }

   public void onDisconnect(String userId, String sessionId) {
      subscriptionsPerUser.compute(userId, (uid, subs) -> {
         if (subs == null) {
            return null;
         }
         List<String> roomIds = subs.remove(sessionId);
         for (String roomId : roomIds) {
            if (!subs.subscrptionExistsForRoom(roomId)) {
               deactivateUser(roomId, userId);
            }
         }
         return subs.isEmpty() ? null : subs;
      });
   }

   private void deactivateUser(String roomId, String userId) {
      var event = roomService.deactivateUser(roomId, userId);
      if (event != null) {
         stompTemplate.sendToRoom(roomId, RoomStompMessage.event(event));
         stompTemplate.sendToRoom(roomId, RoomStompMessage.users(roomService.getActiveUsers(roomId)));
      }
   }

   private static final class UserSubscriptions {

      private final Map<String, Map<String, String>> roomsPerSubscriptionPerSession = new HashMap<>();

      private void add(String sessionId, String subscriptionId, String roomId) {
         roomsPerSubscriptionPerSession.computeIfAbsent(sessionId, id -> new HashMap<>()).put(subscriptionId, roomId);
      }

      private List<String> remove(String sessionId) {
         var roomsPerSubscription = roomsPerSubscriptionPerSession.remove(sessionId);
         return roomsPerSubscription == null ? emptyList() : new ArrayList<>(roomsPerSubscription.values());
      }

      private String remove(String sessionId, String subscriptionId) {
         var holder = new Holder<String>();
         roomsPerSubscriptionPerSession.compute(sessionId, (id, roomsPerSubscription) -> {
            if (roomsPerSubscription == null) {
               return null;
            }
            holder.set(roomsPerSubscription.remove(subscriptionId));
            return roomsPerSubscription.isEmpty() ? null : roomsPerSubscription;
         });
         return holder.get();
      }

      private boolean isEmpty() {
         return roomsPerSubscriptionPerSession.isEmpty();
      }

      private boolean subscrptionExistsForRoom(String roomId) {
         return roomsPerSubscriptionPerSession
               .values()
               .stream()
               .anyMatch(roomsPerSubscription -> roomsPerSubscription.containsValue(roomId));
      }

   }

}
