package com.lib.demo.test;

import com.lib.demo.entity.Book;
import com.lib.demo.entity.Notification;
import com.lib.demo.entity.Reservation;
import com.lib.demo.entity.User;
import com.lib.demo.exception.BusinessException;

import java.util.List;

/**
 * 预约模块测试 — 覆盖 ReservationService 全部方法
 *
 * 黑盒设计方法:
 *   等价类: 图书状态(有可借副本/无可借副本), 预定状态(ACTIVE/CANCELLED/NOTIFIED),
 *          操作者身份(预约者本人/管理员/其他借阅者)
 *
 * 白盒覆盖目标:
 *   路径覆盖: reserveBook 校验分支, cancelReservation 权限+通知下一位路径
 */
public class ReservationServiceTest extends TestBase {

    public static void main(String[] args) {
        ReservationServiceTest t = new ReservationServiceTest();
        t.runAll();
    }

    public void runAll() {
        startModule("预约管理 (ReservationService)");
        passed = 0; failed = 0;

        testReserveBook();          // 黑盒等价类 + 白盒路径
        testCancelReservation();    // 白盒路径覆盖
        testOutOfStockNotifications(); // 黑盒

        printSummary();
    }

    // ==================== 黑盒/白盒: 预定 ====================

    private void testReserveBook() {
        System.out.println("\n  ▸ 黑盒/白盒: 预定图书 (等价类 + 路径覆盖)");

        // 路径: 图书不存在
        test("预定-图书不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getReservationService().reserveBook(borrower.getUserId(), 99999L),
                    "图书不存在应抛异常");
        });

        // 路径: 有可借副本时无需预定
        test("预定-有可借副本时不能预定", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            assertTrue(b.getAvailableCopies() > 0, "预置数据应有可借副本");
            assertThrows(BusinessException.class,
                    () -> ctx.getReservationService().reserveBook(borrower.getUserId(), b.getBookId()),
                    "有可借副本无需预定");
        });

        // 路径: 正常预定(库存为0时)
        test("预定-无库存时正常预定", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            Reservation r = ctx.getReservationService().reserveBook(
                    borrower.getUserId(), b.getBookId());
            assertNotNull(r, "预定应成功");
            assertEquals(Reservation.Status.ACTIVE, r.getStatus(), null);
        });

        // 路径: 重复预定同一本书
        test("预定-不能重复预定同一本", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            ctx.getReservationService().reserveBook(borrower.getUserId(), b.getBookId());
            assertThrows(BusinessException.class,
                    () -> ctx.getReservationService().reserveBook(borrower.getUserId(), b.getBookId()),
                    "重复预定应抛异常");
        });
    }

    // ==================== 白盒: 取消预定 ====================

    private void testCancelReservation() {
        System.out.println("\n  ▸ 白盒: 取消预定 (路径覆盖)");

        // 路径: 预定不存在
        test("取消-预定不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getReservationService().cancelReservation(99999L,
                            borrower.getUserId(), admin),
                    "预定不存在应抛异常");
        });

        // 路径: 非本人且非管理员无权取消
        test("取消-非本人无权取消", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            Reservation r = ctx.getReservationService().reserveBook(
                    borrower.getUserId(), b.getBookId());
            User other = ctx.getUserService().register("cancelOther", "123456", "别人", null, null);
            assertThrows(BusinessException.class,
                    () -> ctx.getReservationService().cancelReservation(
                            r.getReservationId(), other.getUserId(), other),
                    "非本人无权取消");
        });

        // 路径: 本人取消自己的预定
        test("取消-本人取消成功", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            Reservation r = ctx.getReservationService().reserveBook(
                    borrower.getUserId(), b.getBookId());
            ctx.getReservationService().cancelReservation(
                    r.getReservationId(), borrower.getUserId(), borrower);
            Reservation after = ctx.getReservationDao().findById(r.getReservationId());
            assertEquals(Reservation.Status.CANCELLED, after.getStatus(), null);
        });

        // 路径: 管理员取消他人预定
        test("取消-管理员取消他人预定", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            Reservation r = ctx.getReservationService().reserveBook(
                    borrower.getUserId(), b.getBookId());
            ctx.getReservationService().cancelReservation(
                    r.getReservationId(), admin.getUserId(), admin);
            assertEquals(Reservation.Status.CANCELLED,
                    ctx.getReservationDao().findById(r.getReservationId()).getStatus(), null);
        });

        // 路径: 取消已失效的预定
        test("取消-已取消的不能再取消", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            Reservation r = ctx.getReservationService().reserveBook(
                    borrower.getUserId(), b.getBookId());
            ctx.getReservationService().cancelReservation(
                    r.getReservationId(), borrower.getUserId(), borrower);
            assertThrows(BusinessException.class,
                    () -> ctx.getReservationService().cancelReservation(
                            r.getReservationId(), borrower.getUserId(), borrower),
                    "已取消不能再取消");
        });

        // 路径: 取消后通知下一个排队者
        test("取消-自动通知下一位排队者", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            // 两个用户预约同一本书
            User u2 = ctx.getUserService().register("q1", "123456", "排队1", null, null);
            User u3 = ctx.getUserService().register("q2", "123456", "排队2", null, null);
            ctx.getReservationService().reserveBook(u2.getUserId(), b.getBookId());
            ctx.getReservationService().reserveBook(u3.getUserId(), b.getBookId());
            // 找到第一个预约并取消
            List<Reservation> list = ctx.getReservationService().getReservationsForBook(b.getBookId());
            Reservation first = list.get(0);
            ctx.getReservationService().cancelReservation(
                    first.getReservationId(), u2.getUserId(), u2);
            // 第二个预约者应该收到通知
            List<Notification> notifs = ctx.getNotificationService().getAllNotifications(u3.getUserId());
            assertTrue(notifs.size() > 0, "下一个排队者应收到通知");
        });
    }

    // ==================== 黑盒: 缺货通知 ====================

    private void testOutOfStockNotifications() {
        System.out.println("\n  ▸ 黑盒: 缺货通知");

        test("缺货通知-无长期等待时发送0条", () -> {
            setUp(); int count = ctx.getReservationService().sendOutOfStockNotifications();
            assertEquals(0, count, "无长期等待");
        });

        test("缺货通知-有长期等待时发送通知", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            Reservation r = ctx.getReservationService().reserveBook(
                    borrower.getUserId(), b.getBookId());
            // 人为把预约日期改到31天前
            r.setRequestDate(java.time.LocalDate.now().minusDays(31));
            ctx.getReservationDao().update(r);
            int count = ctx.getReservationService().sendOutOfStockNotifications();
            assertEquals(1, count, "1个长期等待→1条通知");
        });
    }
}
