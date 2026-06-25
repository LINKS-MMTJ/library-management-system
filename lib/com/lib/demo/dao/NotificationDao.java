package com.lib.demo.dao;

import com.lib.demo.entity.Notification;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知数据访问对象。
 */
public class NotificationDao extends AbstractDao<Notification> {

    public NotificationDao() {
        super(DataFileNames.NOTIFICATIONS);
    }

    @Override
    protected Long getId(Notification entity) { return entity.getNotificationId(); }

    @Override
    protected void setId(Notification entity, Long id) { entity.setNotificationId(id); }

    @Override
    protected String entityName() { return "通知"; }

    public synchronized List<Notification> findByUserId(Long userId) {
        return dataMap.values().stream()
                .filter(n -> n.getUserId().equals(userId))
                .sorted((n1, n2) -> n2.getSendTime().compareTo(n1.getSendTime()))
                .collect(Collectors.toList());
    }

    public synchronized List<Notification> findUnreadByUserId(Long userId) {
        return dataMap.values().stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .collect(Collectors.toList());
    }
}
