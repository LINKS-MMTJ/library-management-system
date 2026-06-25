package com.lib.demo.test;

import com.lib.demo.entity.Notification;

import java.util.List;

/**
 * 通知模块测试 — 覆盖 NotificationService 全部方法
 *
 * 黑盒设计方法:
 *   等价类: 通知类型(6种), 阅读状态(已读/未读)
 *
 * 白盒覆盖目标:
 *   语句覆盖: send(), getUnreadNotifications(), getAllNotifications(), markAsRead()
 */
public class NotificationServiceTest extends TestBase {

    public static void main(String[] args) {
        NotificationServiceTest t = new NotificationServiceTest();
        t.runAll();
    }

    public void runAll() {
        startModule("消息通知 (NotificationService)");
        passed = 0; failed = 0;

        testSend();                   // 黑盒: 所有通知类型
        testGetNotifications();       // 白盒: 已读/未读区分
        testMarkAsRead();             // 白盒: 标记已读

        printSummary();
    }

    // ==================== 黑盒: 发送通知 ====================

    private void testSend() {
        System.out.println("\n  ▸ 黑盒: 发送通知 (所有通知类型)");

        test("发送-罚金通知", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "测试罚金 ¥5.0", Notification.Type.FINE);
            assertNotNull(n, null);
            assertEquals(Notification.Type.FINE, n.getType(), null);
            assertFalse(n.isRead(), "新通知应为未读");
        });

        test("发送-逾期提醒", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "请归还", Notification.Type.OVERDUE_REMINDER);
            assertEquals(Notification.Type.OVERDUE_REMINDER, n.getType(), null);
        });

        test("发送-预定成功", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "预定成功", Notification.Type.RESERVATION_SUCCESS);
            assertEquals(Notification.Type.RESERVATION_SUCCESS, n.getType(), null);
        });

        test("发送-图书可借", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "可以借了", Notification.Type.BOOK_AVAILABLE);
            assertEquals(Notification.Type.BOOK_AVAILABLE, n.getType(), null);
        });

        test("发送-系统通知", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "系统消息", Notification.Type.SYSTEM);
            assertEquals(Notification.Type.SYSTEM, n.getType(), null);
        });
    }

    // ==================== 白盒: 查询通知 ====================

    private void testGetNotifications() {
        System.out.println("\n  ▸ 白盒: 查询通知 (已读/未读区分)");

        test("查询-无通知时返回空列表", () -> {
            setUp();
            List<Notification> unread = ctx.getNotificationService().getUnreadNotifications(
                    borrower.getUserId());
            assertEquals(0, unread.size(), "新用户应无通知");
        });

        test("查询-发送3条后应都能查到", () -> {
            setUp();
            ctx.getNotificationService().send(borrower.getUserId(), "1", Notification.Type.SYSTEM);
            ctx.getNotificationService().send(borrower.getUserId(), "2", Notification.Type.SYSTEM);
            ctx.getNotificationService().send(borrower.getUserId(), "3", Notification.Type.SYSTEM);
            List<Notification> all = ctx.getNotificationService().getAllNotifications(borrower.getUserId());
            assertEquals(3, all.size(), "应查到3条");
            List<Notification> unread = ctx.getNotificationService().getUnreadNotifications(
                    borrower.getUserId());
            assertEquals(3, unread.size(), "未读也应是3条");
        });

        test("查询-标记已读后未读数减少", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "test", Notification.Type.SYSTEM);
            ctx.getNotificationService().markAsRead(n.getNotificationId());
            List<Notification> unread = ctx.getNotificationService().getUnreadNotifications(
                    borrower.getUserId());
            assertEquals(0, unread.size(), "已读后未读应为0");
            List<Notification> all = ctx.getNotificationService().getAllNotifications(
                    borrower.getUserId());
            assertEquals(1, all.size(), "总数仍为1");
        });
    }

    // ==================== 白盒: 标记已读 ====================

    private void testMarkAsRead() {
        System.out.println("\n  ▸ 白盒: 标记已读 (路径覆盖)");

        test("标读-不存在的ID不抛异常", () -> {
            setUp();
            // 应静默忽略
            ctx.getNotificationService().markAsRead(99999L);
            // 不抛异常即为通过
        });

        test("标读-已读的通知再标读不变", () -> {
            setUp();
            Notification n = ctx.getNotificationService().send(
                    borrower.getUserId(), "t", Notification.Type.SYSTEM);
            ctx.getNotificationService().markAsRead(n.getNotificationId());
            assertTrue(
                    ctx.getNotificationDao().findById(n.getNotificationId()).isRead(),
                    "第一次标读后应为已读");
            ctx.getNotificationService().markAsRead(n.getNotificationId());
            assertTrue(
                    ctx.getNotificationDao().findById(n.getNotificationId()).isRead(),
                    "第二次标读仍为已读");
        });
    }
}
