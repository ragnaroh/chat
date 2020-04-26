package com.ragnaroh.chat.server.services;

import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class UserDao extends Dao {

   Integer getKeyOrNull(String id) {
      return queryForNullableInteger("SELECT KEY FROM User WHERE ID = :id", Map.of("id", id));
   }

   int getKey(String id) {
      return queryForInteger("SELECT KEY FROM User WHERE ID = :id", Map.of("id", id));
   }

   int insertUser(String id) {
      return updateAndReturnId("INSERT INTO User (`ID`) VALUES (:id)", Map.of("id", id));
   }

}
