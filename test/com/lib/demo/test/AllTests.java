package com.lib.demo.test;

/**
 * 图书管理系统 — 总测试入口
 *
 * 运行方式: 在 IDEA 中右键此类 → Run 'AllTests.main()'
 *
 * 测试涵盖:
 *   黑盒测试 — 等价类划分、边界值分析
 *   白盒测试 — 语句覆盖、路径覆盖
 *
 * 覆盖模块:
 *   1. 用户管理 (UserService)
 *   2. 图书管理 (BookService)
 *   3. 借阅管理 (BorrowService)
 *   4. 预约管理 (ReservationService)
 *   5. 消息通知 (NotificationService)
 *
 * 注意: 测试会清除 library_data 目录下的所有 .dat 文件以确保干净环境。
 *       如需保留现有数据，请在运行前备份 library_data 目录。
 */
public class AllTests {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     图书管理系统 — 单元/集成测试套件      ║");
        System.out.println("║     测试方法: 黑盒(等价类+边界值)         ║");
        System.out.println("║              白盒(语句覆盖+路径覆盖)      ║");
        System.out.println("╚══════════════════════════════════════════╝");

        long start = System.currentTimeMillis();

        // 模块1: 用户管理
        UserServiceTest userTest = new UserServiceTest();
        userTest.runAll();
        accumulate(userTest);

        // 模块2: 图书管理
        BookServiceTest bookTest = new BookServiceTest();
        bookTest.runAll();
        accumulate(bookTest);

        // 模块3: 借阅管理
        BorrowServiceTest borrowTest = new BorrowServiceTest();
        borrowTest.runAll();
        accumulate(borrowTest);

        // 模块4: 预约管理
        ReservationServiceTest resTest = new ReservationServiceTest();
        resTest.runAll();
        accumulate(resTest);

        // 模块5: 消息通知
        NotificationServiceTest notifTest = new NotificationServiceTest();
        notifTest.runAll();
        accumulate(notifTest);

        long elapsed = System.currentTimeMillis() - start;

        // 总汇总
        System.out.println("\n\n╔══════════════════════════════════════════╗");
        System.out.println("║            全部测试汇总                   ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  总计: " + pad(String.valueOf(totalPassed + totalFailed), 4)
                + "  通过: [32m" + pad(String.valueOf(totalPassed), 4) + "[0m"
                + "  失败: " + (totalFailed > 0 ? "[31m" : "")
                + pad(String.valueOf(totalFailed), 4)
                + (totalFailed > 0 ? "[0m" : "") + "  ║");
        System.out.println("║  耗时: " + elapsed + " ms                          ║");

        double passRate = (totalPassed + totalFailed) > 0
                ? 100.0 * totalPassed / (totalPassed + totalFailed) : 0;
        System.out.println("║  通过率: " + String.format("%.1f%%", passRate)
                + "                           ║");
        System.out.println("╚══════════════════════════════════════════╝");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static void accumulate(TestBase t) {
        totalPassed += t.passed;
        totalFailed += t.failed;
    }

    private static String pad(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
