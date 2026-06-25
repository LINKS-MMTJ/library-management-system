package com.lib.demo.service;

import com.lib.demo.dao.NotificationDao;
import com.lib.demo.entity.Notification;
import com.lib.demo.util.LogUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

public class NotificationService {
    private static final Logger LOG = LogUtil.getLogger(NotificationService.class);
    private final NotificationDao notificationDao;

    public NotificationService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public Notification send(Long userId, String content, Notification.Type type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setContent(content);
        n.setType(type);
        n.setSendTime(LocalDate.now());
        n.setRead(false);
        notificationDao.save(n);
        LOG.fine("发送通知: userId=" + userId + " type=" + type);
        return n;
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationDao.findUnreadByUserId(userId);
    }

    public List<Notification> getAllNotifications(Long userId) {
        return notificationDao.findByUserId(userId);
    }

    public void markAsRead(Long notificationId) {
        Notification n = notificationDao.findById(notificationId);
        if (n != null) {
            n.setRead(true);
            notificationDao.update(n);
        }
    }
}
