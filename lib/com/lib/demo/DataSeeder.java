package com.lib.demo;

import com.lib.demo.entity.Book;
import com.lib.demo.entity.User;
import com.lib.demo.service.BookService;
import com.lib.demo.service.UserService;
import com.lib.demo.util.PasswordUtil;

import java.time.LocalDate;

/**
 * 测试数据初始化器。
 * 当数据库为空时预填充示例图书和用户，方便演示和测试。
 */
final class DataSeeder {

    private DataSeeder() {}

    /**
     * 当数据库为空时初始化测试数据。
     * @param ctx 应用上下文，提供 DAO 和 Service 访问
     * @return true 表示实际插入了数据，false 表示已有数据跳过
     */
    static boolean seedIfEmpty(AppContext ctx) {
        UserService userService = ctx.getUserService();
        BookService bookService = ctx.getBookService();

        if (!ctx.getUserDao().findAll().isEmpty()) {
            return false;
        }

        // ─── 种子用户（直接通过 DAO 插入，绕过 Service 权限检查） ───
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(PasswordUtil.hash("admin123"));
        admin.setName("系统管理员");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.Status.ACTIVE);
        ctx.getUserDao().save(admin);

        User librarian = new User();
        librarian.setUsername("lib1");
        librarian.setPassword(PasswordUtil.hash("lib123"));
        librarian.setName("图书管理员");
        librarian.setRole(User.Role.LIBRARIAN);
        librarian.setStatus(User.Status.ACTIVE);
        ctx.getUserDao().save(librarian);

        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword(PasswordUtil.hash("user123"));
        user1.setName("张三");
        user1.setRole(User.Role.BORROWER);
        user1.setStatus(User.Status.ACTIVE);
        ctx.getUserDao().save(user1);

        // ─── 种子图书 ───
        bookService.addBook(createBook("9787111213826", "Java编程思想", "Bruce Eckel",
                "机械工业出版社", LocalDate.of(2007, 6, 1), "计算机科学", "A区3排2架"), 5, admin);
        bookService.addBook(createBook("9787115428028", "算法导论", "Thomas H. Cormen",
                "人民邮电出版社", LocalDate.of(2013, 1, 1), "计算机科学", "A区4排1架"), 3, admin);
        bookService.addBook(createBook("9787115221704", "深入理解Java虚拟机", "周志明",
                "人民邮电出版社", LocalDate.of(2011, 6, 1), "计算机科学", "A区2排5架"), 4, admin);
        bookService.addBook(createBook("9787115478559", "数据库系统概论", "王珊",
                "高等教育出版社", LocalDate.of(2017, 2, 1), "计算机科学", "B区1排3架"), 3, admin);
        bookService.addBook(createBook("9787115546074", "计算机网络：自顶向下方法", "James Kurose",
                "机械工业出版社", LocalDate.of(2018, 9, 1), "计算机科学", "B区2排1架"), 3, admin);
        bookService.addBook(createBook("9787020002207", "红楼梦", "曹雪芹",
                "人民文学出版社", LocalDate.of(1996, 12, 1), "文学名著", "C区1排1架"), 6, admin);
        bookService.addBook(createBook("9787544253994", "百年孤独", "加西亚·马尔克斯",
                "南海出版公司", LocalDate.of(2011, 6, 1), "文学名著", "C区1排2架"), 4, admin);
        bookService.addBook(createBook("9787020070657", "活着", "余华",
                "作家出版社", LocalDate.of(2012, 8, 1), "中国文学", "C区2排3架"), 5, admin);
        bookService.addBook(createBook("9787506365437", "三体", "刘慈欣",
                "重庆出版社", LocalDate.of(2008, 1, 1), "科幻小说", "D区1排1架"), 5, admin);
        bookService.addBook(createBook("9787532756124", "百年孤独", "加西亚·马尔克斯",
                "上海译文出版社", LocalDate.of(2011, 6, 1), "外国文学", "C区1排4架"), 2, admin);
        bookService.addBook(createBook("9787201063102", "小王子", "圣埃克苏佩里",
                "天津人民出版社", LocalDate.of(2013, 1, 1), "外国文学", "D区3排2架"), 4, admin);
        bookService.addBook(createBook("9787544242516", "围城", "钱钟书",
                "人民文学出版社", LocalDate.of(1991, 2, 1), "中国文学", "C区2排1架"), 3, admin);
        bookService.addBook(createBook("9787020024759", "骆驼祥子", "老舍",
                "人民文学出版社", LocalDate.of(2000, 5, 1), "中国文学", "C区2排2架"), 4, admin);
        bookService.addBook(createBook("9787111213532", "设计模式：可复用面向对象软件的基础", "Erich Gamma",
                "机械工业出版社", LocalDate.of(2004, 9, 1), "计算机科学", "A区1排1架"), 3, admin);
        bookService.addBook(createBook("9787121029796", "鸟哥的Linux私房菜", "鸟哥",
                "人民邮电出版社", LocalDate.of(2015, 3, 1), "计算机科学", "A区3排1架"), 3, admin);
        bookService.addBook(createBook("9787308083256", "高等数学（第七版）", "同济大学数学系",
                "高等教育出版社", LocalDate.of(2014, 7, 1), "数学", "E区1排1架"), 8, admin);
        bookService.addBook(createBook("9787111187776", "Spring实战", "Craig Walls",
                "人民邮电出版社", LocalDate.of(2016, 3, 1), "计算机科学", "A区1排3架"), 4, admin);

        return true;
    }

    private static Book createBook(String isbn, String title, String author, String publisher,
                                   LocalDate publishDate, String category, String location) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setPublishDate(publishDate);
        book.setCategory(category);
        book.setLocation(location);
        return book;
    }
}
