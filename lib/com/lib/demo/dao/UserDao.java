package com.lib.demo.dao;

import com.lib.demo.entity.User;

import java.util.*;

/**
 * 用户数据访问对象。
 */
public class UserDao extends AbstractDao<User> {

    public UserDao() {
        super(DataFileNames.USERS);
    }

    @Override
    protected Long getId(User entity) { return entity.getUserId(); }

    @Override
    protected void setId(User entity, Long id) { entity.setUserId(id); }

    @Override
    protected String entityName() { return "用户"; }

    public synchronized User findByUsername(String username) {
        return dataMap.values().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
    }

    public synchronized boolean usernameExists(String username) {
        return dataMap.values().stream().anyMatch(u -> username.equals(u.getUsername()));
    }
}
