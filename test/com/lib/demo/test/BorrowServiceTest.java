package com.lib.demo.test;

import com.lib.demo.entity.*;
import com.lib.demo.exception.BusinessException;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅模块测试 — 覆盖 BorrowService 全部方法
 *
 * 黑盒设计方法:
 *   等价类: 用户角色(借阅者/管理员/图书管理员), 图书状态(可借/无库存),
 *          用户状态(正常/禁用/有逾期/有罚金), 借阅记录状态(借阅中/已归还/逾期)
 *   边界值: 逾期天数=0/1/30/31, 罚金=0/0.5/15, 续借次数=0/1/2/3
 *
 * 白盒覆盖目标:
 *   路径覆盖: borrowBook 所有校验分支, returnBook 罚金计算分支,
 *           renewBook 权限+次数+被预约分支
 */
public class BorrowServiceTest extends TestBase {

    public static void main(String[] args) {
        BorrowServiceTest t = new BorrowServiceTest();
        t.runAll();
    }

    public void runAll() {
        startModule("借阅管理 (BorrowService)");
        passed = 0; failed = 0;

        testBorrowBook();           // 黑盒等价类 + 白盒全路径
        testReturnBook();           // 白盒按recordId归还 + 罚金
        testRenewBook();            // 白盒续借路径
        testCalculateFine();        // 黑盒边界值
        testOverdueReminders();     // 白盒逾期提醒

        printSummary();
    }

    // ==================== 黑盒/白盒: 借书 ====================

    private void testBorrowBook() {
        System.out.println("\n  ▸ 黑盒/白盒: 借阅图书 (等价类 + 全路径覆盖)");

        // --- 用户不存在 ---
        test("借书-用户不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(99999L, 1L, admin),
                    "用户不存在应抛异常");
        });

        // --- 图书不存在 ---
        test("借书-图书不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(borrower.getUserId(), 99999L, admin),
                    "图书不存在应抛异常");
        });

        // --- 角色限制: 管理员/图书管理员不能借阅 ---
        test("借书-管理员不能借阅", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(admin.getUserId(), b.getBookId(), admin),
                    "管理员不能借阅");
        });

        test("借书-图书管理员不能借阅", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(librarian.getUserId(), b.getBookId(), librarian),
                    "图书管理员不能借阅");
        });

        // --- 借阅者只能为自己借 ---
        test("借书-借阅者不能代他人借", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            // 创建另一个借阅者
            User other = ctx.getUserService().register("other", "123456", "其他人", null, null);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(other.getUserId(), b.getBookId(), borrower),
                    "借阅者(borrower)不能代借");
        });

        // --- 正常借阅 ---
        test("借书-正常借阅成功", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            int oldAvail = b.getAvailableCopies();
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            assertNotNull(r, "借阅应成功");
            assertEquals(BorrowRecord.Status.BORROWED, r.getStatus(), null);
            // 可用副本应-1
            Book after = ctx.getBookService().getBookById(b.getBookId());
            assertEquals((Integer)(oldAvail - 1), after.getAvailableCopies(), null);
        });

        // --- 无库存 ---
        test("借书-无可用副本应失败", () -> {
            setUp();
            // 找一本书，把availableCopies设为0
            Book b = ctx.getBookService().getAllBooks().get(0);
            b.setAvailableCopies(0);
            ctx.getBookDao().update(b);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(borrower.getUserId(), b.getBookId(), borrower),
                    "无库存应抛异常");
        });

        // --- 账户异常 ---
        test("借书-用户被禁用", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            ctx.getUserService().disableUser(borrower.getUserId(), admin);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(borrower.getUserId(), b.getBookId(), borrower),
                    "禁用用户无法借阅");
        });

        // --- 重复借阅同一本书 ---
        test("借书-不能重复借同一本", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            ctx.getBorrowService().borrowBook(borrower.getUserId(), b.getBookId(), borrower);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(borrower.getUserId(), b.getBookId(), borrower),
                    "重复借同一本应抛异常");
        });

        // --- 有逾期未还 ---
        test("借书-有逾期记录时不能借", () -> {
            setUp();
            // 先借一本并制造逾期
            Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            r.setDueDate(LocalDate.now().minusDays(1)); // 昨天到期
            ctx.getBorrowRecordDao().update(r);
            // 尝试借另一本
            Book b2 = ctx.getBookService().getAllBooks().get(1);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(borrower.getUserId(), b2.getBookId(), borrower),
                    "有逾期应抛异常");
        });

        // --- 有未缴罚金 ---
        test("借书-有罚金时不能借", () -> {
            setUp();
            borrower.setUnpaidFine(5.0);
            ctx.getUserDao().update(borrower);
            Book b = ctx.getBookService().getAllBooks().get(0);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().borrowBook(borrower.getUserId(), b.getBookId(), borrower),
                    "有罚金应抛异常");
        });
    }

    // ==================== 白盒: 归还 ====================

    private void testReturnBook() {
        System.out.println("\n  ▸ 白盒: 归还图书 (路径覆盖)");

        test("归还-记录不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().returnBookByRecordId(99999L, admin),
                    "不存在记录应抛异常");
        });

        test("归还-已归还的记录", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            ctx.getBorrowService().returnBookByRecordId(r.getRecordId(), admin);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().returnBookByRecordId(r.getRecordId(), admin),
                    "已归还的记录应抛异常");
        });

        test("归还-正常归还(未逾期)", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            int oldAvail = b.getAvailableCopies();
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            BorrowRecord ret = ctx.getBorrowService().returnBookByRecordId(r.getRecordId(), admin);
            assertTrue(ret.isReturned(), "应已归还");
            // 库存恢复
            Book after = ctx.getBookService().getBookById(b.getBookId());
            assertEquals((Integer)oldAvail, after.getAvailableCopies(), null);
        });

        test("归还-普通借阅者不能归还别人的书", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            User other = ctx.getUserService().register("zother", "123456", "别人", null, null);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().returnBookByRecordId(r.getRecordId(), other),
                    "不能归还别人的书");
        });

        // 逾期归还产生罚金
        test("归还-逾期归还自动产生罚金", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            r.setDueDate(LocalDate.now().minusDays(5));
            ctx.getBorrowRecordDao().update(r);
            BorrowRecord ret = ctx.getBorrowService().returnBookByRecordId(r.getRecordId(), admin);
            double fine = ctx.getBorrowService().calculateFine(ret);
            assertTrue(fine > 0, "逾期应有罚金: " + fine);
            assertEquals(5 * 0.5, fine, 0.001, "5天逾期=¥2.5");
            User u = ctx.getUserDao().findById(borrower.getUserId());
            assertEquals(fine, u.getUnpaidFine(), 0.001, "罚金已累加到用户");
        });
    }

    // ==================== 白盒: 续借 ====================

    private void testRenewBook() {
        System.out.println("\n  ▸ 白盒: 续借 (路径覆盖)");

        test("续借-记录不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().renewBook(99999L, borrower),
                    "记录不存在应抛异常");
        });

        test("续借-只能续借自己的书", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            User other = ctx.getUserService().register("oth2", "123456", "别人2", null, null);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().renewBook(r.getRecordId(), other),
                    "不能续借别人的书");
        });

        test("续借-正常续借成功", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            LocalDate oldDue = r.getDueDate();
            BorrowRecord renewed = ctx.getBorrowService().renewBook(r.getRecordId(), borrower);
            assertEquals(oldDue.plusDays(15), renewed.getDueDate(), "应延长15天");
            assertEquals(1, renewed.getRenewCount(), "续借次数=1");
        });

        test("续借-边界值: 第2次续借(最大次数)", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            ctx.getBorrowService().renewBook(r.getRecordId(), borrower);
            // 第2次
            BorrowRecord r2 = ctx.getBorrowService().renewBook(r.getRecordId(), borrower);
            assertEquals(2, r2.getRenewCount(), "续借次数=2");
        });

        test("续借-边界值: 第3次续借应失败(>最大2次)", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            ctx.getBorrowService().renewBook(r.getRecordId(), borrower);
            ctx.getBorrowService().renewBook(r.getRecordId(), borrower);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().renewBook(r.getRecordId(), borrower),
                    "超过2次续借应抛异常");
        });

        test("续借-已归还的书不能续借", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            ctx.getBorrowService().returnBookByRecordId(r.getRecordId(), admin);
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().renewBook(r.getRecordId(), borrower),
                    "已归还不能续借");
        });

        test("续借-被他人预定则不能续借", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            // 另一用户预定了此书(必须先让库存为0)
            b.setAvailableCopies(0); ctx.getBookDao().update(b);
            User other = ctx.getUserService().register("ot3", "123456", "别3", null, null);
            ctx.getReservationService().reserveBook(other.getUserId(), b.getBookId());
            // 恢复库存让续借路径走到"被预约"检查
            b.setAvailableCopies(10); ctx.getBookDao().update(b);
            // 现在书已被别人预定
            assertThrows(BusinessException.class,
                    () -> ctx.getBorrowService().renewBook(r.getRecordId(), borrower),
                    "被他人预定应抛异常");
        });
    }

    // ==================== 黑盒: 罚金计算 ====================

    private void testCalculateFine() {
        System.out.println("\n  ▸ 黑盒: 罚金计算 (边界值分析)");

        test("罚金-边界: record为null", () -> {
            setUp(); assertEquals(0.0, ctx.getBorrowService().calculateFine(null), 0.001, "null=0");
        });

        test("罚金-边界: dueDate为null", () -> {
            setUp(); BorrowRecord r = new BorrowRecord();
            assertEquals(0.0, ctx.getBorrowService().calculateFine(r), 0.001, "dueDate=null → 0");
        });

        test("罚金-边界: 逾期0天(刚好到期日)", () -> {
            setUp(); BorrowRecord r = new BorrowRecord();
            r.setDueDate(LocalDate.now());
            r.setReturnDate(LocalDate.now());
            assertEquals(0.0, ctx.getBorrowService().calculateFine(r), 0.001, "0天逾期=0");
        });

        test("罚金-边界: 逾期1天", () -> {
            setUp(); BorrowRecord r = new BorrowRecord();
            r.setDueDate(LocalDate.now().minusDays(1));
            r.setReturnDate(LocalDate.now());
            assertEquals(0.5, ctx.getBorrowService().calculateFine(r), 0.001, "1天=¥0.5");
        });

        test("罚金-边界: 逾期30天", () -> {
            setUp(); BorrowRecord r = new BorrowRecord();
            r.setDueDate(LocalDate.now().minusDays(30));
            r.setReturnDate(LocalDate.now());
            assertEquals(15.0, ctx.getBorrowService().calculateFine(r), 0.001, "30天=¥15");
        });
    }

    // ==================== 白盒: 逾期提醒 ====================

    private void testOverdueReminders() {
        System.out.println("\n  ▸ 白盒: 逾期提醒发送");

        test("逾期提醒-无逾期时发送0条", () -> {
            setUp(); int count = ctx.getBorrowService().sendOverdueReminders();
            assertEquals(0, count, "无逾期记录应为0");
        });

        test("逾期提醒-有逾期时正确发送", () -> {
            setUp();
            Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            r.setDueDate(LocalDate.now().minusDays(1));
            ctx.getBorrowRecordDao().update(r);
            int count = ctx.getBorrowService().sendOverdueReminders();
            assertEquals(1, count, "1条逾期→发送1条提醒");
        });

        test("逾期提醒-单用户检查", () -> {
            setUp();
            boolean has = ctx.getBorrowService().sendOverdueReminderToUser(borrower.getUserId());
            assertFalse(has, "新用户无逾期");

            Book b = ctx.getBookService().getAllBooks().get(0);
            BorrowRecord r = ctx.getBorrowService().borrowBook(
                    borrower.getUserId(), b.getBookId(), borrower);
            r.setDueDate(LocalDate.now().minusDays(1));
            ctx.getBorrowRecordDao().update(r);
            has = ctx.getBorrowService().sendOverdueReminderToUser(borrower.getUserId());
            assertTrue(has, "有逾期记录应返回true");
        });
    }
}
