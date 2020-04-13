package com.ragnaroh.chat.server.web.servlet.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ragnaroh.chat.server.services.RoomService;

@Component
public class RoomSubscriptionHelper {

   private final ConcurrentMap<String, Pair<String, String>> subscriptions = new ConcurrentHashMap<>();

   @Autowired
   private RoomService roomService;

   public void onSubscribe(String subscriptionId, String userId, String roomId) {
      subscriptions.put(subscriptionId, Pair.of(roomId, userId));
   }

   public void onUnsubscribe(String subscriptionId) {
      var pair = subscriptions.remove(subscriptionId);
      if (pair != null) {
         roomService.removeUser(pair.getLeft(), pair.getRight());
      }
   }

}
