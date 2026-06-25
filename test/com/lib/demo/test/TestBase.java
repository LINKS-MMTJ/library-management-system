package com.lib.demo.test;

import com.lib.demo.AppContext;
import com.lib.demo.entity.User;
import java.io.File;

/**
 * 测试基类 — 提供测试环境初始化与统计工具
 */
public class TestBase {
    protected AppContext ctx;
    protected User admin;
    protected User librarian;
    protected User borrower;
    protected int passed = 0;
    protected int failed = 0;
    protected String currentModule;

    public void setUp() {
        // 清除旧数据，确保每次测试从干净状态开始
        cleanDataFiles();
        ctx = new AppContext();
        admin = ctx.getUserDao().findByUsername("admin");
        librarian = ctx.getUserDao().findByUsername("lib1");
        borrower = ctx.getUserDao().findByUsername("user1");
    }

    private void cleanDataFiles() {
        String[] files = {"books.dat", "users.dat", "borrow_records.dat",
                "reservations.dat", "notifications.dat", "id_state.ser"};
        for (String f : files) {
            File file = new File("library_data", f);
            if (file.exists()) file.delete();
        }
    }

    protected void startModule(String name) {
        currentModule = name;
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║  测试模块: " + padRight(name, 26) + "║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    protected void test(String caseName, Runnable testLogic) {
        try {
            testLogic.run();
            passed++;
            System.out.println("  ✓ PASS — " + caseName);
        } catch (AssertionError e) {
            failed++;
            System.out.println("  ✗ FAIL — " + caseName);
            System.out.println("      原因: " + e.getMessage());
        } catch (Exception e) {
            failed++;
            System.out.println("  ✗ ERROR — " + caseName);
            System.out.println("      异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ========== 断言工具 ==========

    protected void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError(msg == null ? "期望非null, 实际为null" : msg);
    }

    protected void assertNull(Object obj, String msg) {
        if (obj != null) throw new AssertionError(msg == null ? "期望null, 实际非null" : msg);
    }

    protected void assertEquals(Object expected, Object actual, String msg) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError((msg == null ? "" : msg + " — ")
                    + "期望: " + expected + ", 实际: " + actual);
        }
    }

    protected void assertEquals(double expected, double actual, double delta, String msg) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError((msg == null ? "" : msg + " — ")
                    + "期望: " + expected + ", 实际: " + actual);
        }
    }

    protected void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg == null ? "期望true, 实际false" : msg);
    }

    protected void assertFalse(boolean condition, String msg) {
        if (condition) throw new AssertionError(msg == null ? "期望false, 实际true" : msg);
    }

    protected void assertThrows(Class<? extends Exception> expectedType, Runnable runnable, String msg) {
        try {
            runnable.run();
            throw new AssertionError(msg == null ? "期望抛出异常 " + expectedType.getSimpleName() + ", 但未抛出" : msg);
        } catch (Exception e) {
            if (!expectedType.isAssignableFrom(e.getClass())) {
                throw new AssertionError((msg == null ? "" : msg + " — ")
                        + "期望异常: " + expectedType.getSimpleName()
                        + ", 实际: " + e.getClass().getSimpleName());
            }
        }
    }

    // ========== 统计输出 ==========

    protected void printSummary() {
        int total = passed + failed;
        System.out.println("\n┌──────────────────────────────────────┐");
        System.out.println("│  [" + currentModule + "] 测试结果                   │");
        System.out.println("│  总计: " + padRight(String.valueOf(total), 4)
                + " 通过: " + padRight(String.valueOf(passed), 4)
                + " 失败: " + padRight(String.valueOf(failed), 4) + "│");
        System.out.println("└──────────────────────────────────────┘");
    }

    protected static String padRight(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
