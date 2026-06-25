package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 系统控制器 — 长期未还提醒/缺货通知等系统操作。
 */
class SystemController {
    /** 长期借阅阈值：90 天（约3个月） */
    private static final int LONG_TERM_DAYS = 90;
    /** 提前警告：借阅超过 75 天开始提醒 */
    private static final int WARNING_AHEAD_DAYS = 15;

    private final AppContext ctx;

    SystemController(AppContext ctx) { this.ctx = ctx; }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        ApiServer.requireAdminOnly(user);
        String action = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "overdue-reminders":
                return sendLongTermReminders(user);
            case "out-of-stock":
                return sendOutOfStockNotifications(user);
            default:
                throw new ApiServer.BusinessHttpException(404, "未知系统操作: " + action);
        }
    }

    /** 长期未还提醒：借阅 ≥ 3 个月的超期提醒 + 即将到期提醒 */
    private String sendLongTermReminders(User operator) {
        int[] result = ctx.getBorrowService().sendLongTermReminders(LONG_TERM_DAYS, WARNING_AHEAD_DAYS);
        return "{\"code\":200,\"data\":{\"overdueCount\":" + result[0]
                + ",\"warningCount\":" + result[1]
                + ",\"message\":\"已发送超期提醒 " + result[0] + " 条，即将到期提醒 " + result[1] + " 条\"}}";
    }

    /** 发送缺货通知：通知等待超时的预约用户 + 抄送管理员 */
    private String sendOutOfStockNotifications(User operator) {
        int count = ctx.getReservationService().sendOutOfStockNotifications();

        // 额外通知所有管理员/图书管理员，方便他们掌握缺货情况
        List<User> allUsers = ctx.getUserDao().findAll();
        int adminNotified = 0;
        for (User u : allUsers) {
            if (u.isAdmin() || u.isLibrarian()) {
                ctx.getNotificationService().send(u.getUserId(),
                        "【缺货汇总】系统已向 " + count + " 位预约用户发送缺货通知，请关注图书采购进展。",
                        com.lib.demo.entity.Notification.Type.OUT_OF_STOCK);
                adminNotified++;
            }
        }

        return "{\"code\":200,\"data\":{\"count\":" + count
                + ",\"adminNotified\":" + adminNotified
                + ",\"message\":\"已发送 " + count + " 条缺货通知，并抄送 " + adminNotified + " 位管理员\"}}";
    }
}
