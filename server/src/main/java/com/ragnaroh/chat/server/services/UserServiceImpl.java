package com.ragnaroh.chat.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

   @Autowired
   private UserDao userDao;

   @Override
   public Integer getUserKey(String userId) {
      return userDao.getKeyOrNull(userId);
   }

}
