package com.lib.demo.test;

import com.lib.demo.entity.Book;
import com.lib.demo.exception.BusinessException;

import java.util.List;

/**
 * 图书模块测试 — 覆盖 BookService 全部方法
 *
 * 黑盒设计方法:
 *   等价类划分: 权限(管理员/图书管理员/借阅者), ISBN(空/有效/重复), 数量(正/零/负/超库存)
 *   边界值分析: quantity=0/1/-1 边界, 入库/出库数量 vs 库存
 *
 * 白盒覆盖目标:
 *   语句覆盖: addBook(新书/存量追加), removeBook(部分/全部/超额)
 *   路径覆盖: 每个权限判断和异常路径
 */
public class BookServiceTest extends TestBase {

    public static void main(String[] args) {
        BookServiceTest t = new BookServiceTest();
        t.runAll();
    }

    public void runAll() {
        startModule("图书管理 (BookService)");
        passed = 0; failed = 0;

        testAddBook();           // 黑盒等价类 + 边界值
        testRemoveBook();        // 黑盒边界值 + 白盒路径
        testUpdateBookInfo();    // 白盒路径
        testSearchBooks();       // 黑盒等价类
        testGetBookById();       // 边界值

        printSummary();
    }

    // ==================== 黑盒: 添加上架 ====================

    private void testAddBook() {
        System.out.println("\n  ▸ 黑盒/白盒: 新书上架 (等价类 + 路径覆盖)");

        // --- 权限等价类 ---
        test("上架-借阅者无权限", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().addBook(makeBook("x", "x"), 1, borrower),
                    "借阅者无权限上架");
        });

        test("上架-图书管理员有权限", () -> {
            setUp(); Book b = ctx.getBookService().addBook(makeBook("978-1", "测试书A"), 3, librarian);
            assertNotNull(b, "addBook返回不应为null"); assertEquals((Integer)3, b.getTotalCopies(), null);
        });

        // --- ISBN等价类 ---
        test("上架-ISBN为null", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().addBook(makeBook(null, "无ISBN"), 1, admin),
                    "ISBN为null应抛异常");
        });

        test("上架-ISBN为空串", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().addBook(makeBook("  ", "空ISBN"), 1, admin),
                    "ISBN为空串应抛异常");
        });

        // --- 书名等价类 ---
        test("上架-书名为空", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().addBook(makeBook("978-x", "  "), 1, admin),
                    "书名为空应抛异常");
        });

        // --- 数量边界值 ---
        test("上架-边界值: quantity=0 (无效)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().addBook(makeBook("978-a", "A"), 0, admin),
                    "入库数量=0应抛异常");
        });

        test("上架-边界值: quantity=-1 (无效)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().addBook(makeBook("978-b", "B"), -1, admin),
                    "入库数量=-1应抛异常");
        });

        test("上架-边界值: quantity=1 (有效)", () -> {
            setUp(); Book b = ctx.getBookService().addBook(makeBook("978-c", "C"), 1, admin);
            assertEquals(1, b.getTotalCopies(), null);
            assertEquals(1, b.getAvailableCopies(), null);
        });

        // --- 存量追加路径 (白盒: ISBN已存在走更新分支) ---
        test("上架-ISBN已存在时应追加库存", () -> {
            setUp();
            // 获取一本已有图书的ISBN
            Book existing = ctx.getBookService().getAllBooks().get(0);
            String isbn = existing.getIsbn();
            int oldTotal = existing.getTotalCopies();
            Book result = ctx.getBookService().addBook(makeBook(isbn, existing.getTitle()), 5, admin);
            assertEquals((Integer)(oldTotal + 5), result.getTotalCopies(),
                    "存量追加后总数应+5");
        });
    }

    // ==================== 黑盒/白盒: 出库下架 ====================

    private void testRemoveBook() {
        System.out.println("\n  ▸ 黑盒/白盒: 图书出库 (边界值 + 路径覆盖)");

        test("出库-权限不足", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().removeBook(1L, 1, "test", borrower),
                    "借阅者无权出库");
        });

        test("出库-图书不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().removeBook(99999L, 1, "z", admin),
                    "不存在的图书应抛异常");
        });

        test("出库-边界值: quantity=0", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().removeBook(b.getBookId(), 0, "无效", admin),
                    "出库数量=0应抛异常");
        });

        test("出库-边界值: quantity>库存总量", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().removeBook(b.getBookId(),
                            b.getTotalCopies() + 10, "超出", admin),
                    "出库数量>总库存应抛异常");
        });

        test("出库-部分出库(剩余>0)", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            int oldTotal = b.getTotalCopies();
            ctx.getBookService().removeBook(b.getBookId(), 1, "破损", admin);
            Book after = ctx.getBookService().getBookById(b.getBookId());
            assertEquals((Integer)(oldTotal - 1), after.getTotalCopies(), null);
        });

        // P2: 测试仅剩1本且库存全在时出库全部 → 图书应被删除
        test("出库-边...全部出库后图书被删除", () -> {
            setUp();
            // 先上架一本只有1册的书
            Book nb = ctx.getBookService().addBook(makeBook("978-del", "待删除"), 1, admin);
            ctx.getBookService().removeBook(nb.getBookId(), 1, "清空", admin);
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().getBookById(nb.getBookId()),
                    "全部出库后应找不到该书");
        });
    }

    // ==================== 白盒: 更新图书信息 ====================

    private void testUpdateBookInfo() {
        System.out.println("\n  ▸ 白盒: 更新图书信息 (路径覆盖)");

        test("更新-权限不足", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().updateBookInfo(1L, new Book(), borrower),
                    "借阅者无权更新");
        });

        test("更新-图书不存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().updateBookInfo(99999L, new Book(), admin),
                    "图书不存在应抛异常");
        });

        test("更新-修改书名", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            Book update = new Book(); update.setTitle("新书名");
            Book after = ctx.getBookService().updateBookInfo(b.getBookId(), update, admin);
            assertEquals("新书名", after.getTitle(), null);
        });

        test("更新-修改分类和位置", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            Book update = new Book();
            update.setCategory("新分类"); update.setLocation("新位置");
            Book after = ctx.getBookService().updateBookInfo(b.getBookId(), update, admin);
            assertEquals("新分类", after.getCategory(), null);
            assertEquals("新位置", after.getLocation(), null);
        });
    }

    // ==================== 黑盒: 搜索 ====================

    private void testSearchBooks() {
        System.out.println("\n  ▸ 黑盒: 图书搜索 (等价类划分)");

        test("搜索-null关键词返回全部", () -> {
            setUp(); List<Book> r = ctx.getBookService().searchBooks(null);
            assertTrue(r.size() >= 17, "null应返回全部图书");
        });

        test("搜索-空串返回全部", () -> {
            setUp(); List<Book> r = ctx.getBookService().searchBooks("  ");
            assertTrue(r.size() >= 17, "空串应返回全部");
        });

        test("搜索-按书名命中", () -> {
            setUp(); List<Book> r = ctx.getBookService().searchBooks("Java编程思想");
            assertEquals(1, r.size(), null);
        });

        test("搜索-按作者命中", () -> {
            setUp(); List<Book> r = ctx.getBookService().searchBooks("余华");
            assertTrue(r.size() >= 1, "按作者应命中");
        });

        test("搜索-按ISBN命中", () -> {
            setUp(); List<Book> r = ctx.getBookService().searchBooks("9787111213826");
            assertEquals(1, r.size(), null);
        });

        test("搜索-无匹配关键词", () -> {
            setUp(); List<Book> r = ctx.getBookService().searchBooks("不存在书名xyz999");
            assertEquals(0, r.size(), "无匹配应返回空列表");
        });
    }

    // ==================== 边界值: getBookById ====================

    private void testGetBookById() {
        System.out.println("\n  ▸ 边界值测试: getBookById");

        test("按ID查询-存在", () -> {
            setUp(); Book b = ctx.getBookService().getAllBooks().get(0);
            Book found = ctx.getBookService().getBookById(b.getBookId());
            assertNotNull(found, null);
        });

        test("按ID查询-不存在应抛异常", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getBookService().getBookById(99999L),
                    "不存在ID应抛异常");
        });
    }

    // ==================== 辅助 ====================

    private Book makeBook(String isbn, String title) {
        Book b = new Book();
        b.setIsbn(isbn); b.setTitle(title); b.setAuthor("测试");
        return b;
    }
}
