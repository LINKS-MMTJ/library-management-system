package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.Notification;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;

import java.util.*;

/**
 * 通知控制器 — 列表/未读数/标记已读。
 */
class NotificationController {
    private final AppContext ctx;

    NotificationController(AppContext ctx) { this.ctx = ctx; }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        ApiServer.requireAuth(user);
        Long id = ApiServer.parseIdParam(parts, 1);

        if (parts.length >= 3 && id != null && "read".equals(parts[2])) {
            return markAsRead(id);
        }

        if ("unread-count".equals(parts.length > 1 ? parts[1] : "")) {
            return unreadCount(user);
        }

        return listAll(user);
    }

    private String listAll(User user) {
        List<Notification> list = ctx.getNotificationService().getAllNotifications(user.getUserId());
        list.sort((a, b) -> b.getSendTime().compareTo(a.getSendTime()));
        return JsonUtil.listJson(list);
    }

    private String unreadCount(User user) {
        int count = ctx.getNotificationService().getUnreadNotifications(user.getUserId()).size();
        return "{\"code\":200,\"data\":{\"count\":" + count + "}}";
    }

    private String markAsRead(Long id) {
        ctx.getNotificationService().markAsRead(id);
        return JsonUtil.successMsg("已标记");
    }
}
