package com.lib.demo;

import com.lib.demo.dao.*;
import com.lib.demo.service.*;

/**
 * 应用上下文 —— 手工依赖注入容器（组合根）。
 * 负责创建所有组件并组装依赖关系。
 */
public class AppContext {
    // ── DAO（包级可见，仅通过 getter 访问） ──
    private final BookDao bookDao = new BookDao();
    private final UserDao userDao = new UserDao();
    private final BorrowRecordDao borrowRecordDao = new BorrowRecordDao();
    private final ReservationDao reservationDao = new ReservationDao();
    private final NotificationDao notificationDao = new NotificationDao();

    // ── Service ──
    private final BookService bookService;
    private final UserService userService;
    private final BorrowService borrowService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;

    /** 工厂方法：创建并初始化上下文（含种子数据） */
    public static AppContext create() {
        AppContext ctx = new AppContext();
        DataSeeder.seedIfEmpty(ctx);
        return ctx;
    }

    /** 包级可见构造器（测试可直接 new 以跳过种子数据） */
    AppContext() {
        NotificationService ns = new NotificationService(notificationDao);

        this.borrowService = new BorrowService(borrowRecordDao, bookDao, userDao, reservationDao);
        this.reservationService = new ReservationService(reservationDao, bookDao);
        this.notificationService = ns;
        this.bookService = new BookService(bookDao, borrowRecordDao);
        this.userService = new UserService(userDao);

        this.borrowService.setNotificationService(ns);
        this.reservationService.setNotificationService(ns);
    }

    // ── Getter ──

    public BookDao getBookDao()              { return bookDao; }
    public UserDao getUserDao()              { return userDao; }
    public BorrowRecordDao getBorrowRecordDao() { return borrowRecordDao; }
    public ReservationDao getReservationDao()   { return reservationDao; }
    public NotificationDao getNotificationDao() { return notificationDao; }

    public BookService getBookService()                    { return bookService; }
    public UserService getUserService()                    { return userService; }
    public BorrowService getBorrowService()                { return borrowService; }
    public ReservationService getReservationService()      { return reservationService; }
    public NotificationService getNotificationService()    { return notificationService; }
}
