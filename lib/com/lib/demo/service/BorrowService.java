package com.lib.demo.service;

import com.lib.demo.dao.BookDao;
import com.lib.demo.dao.BorrowRecordDao;
import com.lib.demo.dao.ReservationDao;
import com.lib.demo.dao.UserDao;
import com.lib.demo.entity.Book;
import com.lib.demo.entity.BorrowRecord;
import com.lib.demo.entity.Notification;
import com.lib.demo.entity.Reservation;
import com.lib.demo.entity.User;
import com.lib.demo.exception.BusinessException;
import com.lib.demo.util.LogUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BorrowService {
    private static final Logger LOG = LogUtil.getLogger(BorrowService.class);
    private static final int MAX_BORROW_DAYS = 30;
    private static final long FINE_PER_DAY = 50;  // 每天 ¥0.50 = 50 分
    private static final int MAX_RENEWALS = 2;
    private static final int RENEWAL_EXTENSION_DAYS = 15;

    private final BorrowRecordDao borrowRecordDao;
    private final BookDao bookDao;
    private final UserDao userDao;
    private final ReservationDao reservationDao;
    private NotificationService notificationService;

    public BorrowService(BorrowRecordDao borrowRecordDao, BookDao bookDao,
                         UserDao userDao, ReservationDao reservationDao) {
        this.borrowRecordDao = borrowRecordDao;
        this.bookDao = bookDao;
        this.userDao = userDao;
        this.reservationDao = reservationDao;
    }

    public void setNotificationService(NotificationService ns) {
        this.notificationService = ns;
    }

    public BorrowRecord borrowBook(Long userId, Long bookId, User operator) {
        User user = userDao.findById(userId);
        Book book = bookDao.findById(bookId);
        if (user == null) throw new BusinessException("用户不存在");
        if (book == null) throw new BusinessException("图书不存在");
        if (!user.isBorrower()) throw new BusinessException("管理员和图书管理员不能借阅图书，请使用借阅者账户");
        // 防御性检查：借阅者只能为自己借书
        if (operator != null && operator.isBorrower() && !operator.getUserId().equals(userId)) {
            throw new BusinessException("只能为自己借阅图书");
        }
        if (!user.isActive()) throw new BusinessException("账户状态异常，无法借阅");
        if (book.getAvailableCopies() <= 0) throw new BusinessException("该图书暂无可借副本");

        // P1-1: 同用户不能重复借同一本书
        boolean alreadyBorrowed = borrowRecordDao.findUnreturnedByUserId(userId).stream()
                .anyMatch(r -> r.getBookId().equals(bookId));
        if (alreadyBorrowed) throw new BusinessException("您已借阅过此书，请先归还后再借");

        if (borrowRecordDao.hasOverdueRecords(userId)) {
            throw new BusinessException("您有图书已逾期未还，需先归还后才能继续借阅");
        }
        if (user.getUnpaidFine() > 0) {
            throw new BusinessException("您有未支付的罚金 ¥" + centsToYuan(user.getUnpaidFine()) + "，需先处理");
        }

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(MAX_BORROW_DAYS));
        record.setStatus(BorrowRecord.Status.BORROWED);
        borrowRecordDao.save(record);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookDao.update(book);

        cancelReservationIfBorrowed(bookId, userId);

        LOG.info("借书: 用户=" + user.getUsername() + " 图书=《" + book.getTitle() + "》" +
                " 归还日期=" + record.getDueDate());
        return record;
    }

    /**
     * 按借阅记录ID归还（修复P0-1：多副本时避免错还他人记录）
     */
    public BorrowRecord returnBookByRecordId(Long recordId, User operator) {
        BorrowRecord record = borrowRecordDao.findById(recordId);
        if (record == null) throw new BusinessException("借阅记录不存在");
        if (record.isReturned()) throw new BusinessException("该图书已归还");

        // 所有权校验：普通借阅者只能归还自己的书
        if (!isOperator(operator) && !operator.getUserId().equals(record.getUserId())) {
            throw new BusinessException("无权操作此借阅记录");
        }

        Book book = bookDao.findById(record.getBookId());
        record.returnBook();

        long fine = calculateFine(record);
        if (fine > 0) {
            record.setFineAmount(fine);
            User user = userDao.findById(record.getUserId());
            if (user != null) {
                user.setUnpaidFine(user.getUnpaidFine() + fine);
                userDao.update(user);
            }
            if (notificationService != null) {
                notificationService.send(record.getUserId(), "您有一笔逾期罚金: ¥" +
                        centsToYuan(fine) + "，请及时缴纳。", Notification.Type.FINE);
            }
        }
        borrowRecordDao.update(record);

        if (book != null) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookDao.update(book);
            notifyNextReservation(book.getBookId());
        }

        LOG.info("还书: recordId=" + recordId + " 图书=《" + (book != null ? book.getTitle() : "未知") +
                "》 罚金=" + centsToYuan(fine));
        return record;
    }

    public BorrowRecord renewBook(Long recordId, User operator) {
        BorrowRecord record = borrowRecordDao.findById(recordId);
        if (record == null) throw new BusinessException("借阅记录不存在");
        // B8修复：权限校验——只能续借自己的书，管理员/图书管理员无权续借
        if (operator == null || !operator.getUserId().equals(record.getUserId())) {
            throw new BusinessException("只能续借自己借阅的图书");
        }
        if (record.getStatus() != BorrowRecord.Status.BORROWED) {
            throw new BusinessException("该图书已归还，无法续借");
        }
        if (reservationDao.hasActiveReservation(record.getBookId(), record.getUserId())) {
            throw new BusinessException("该书已被他人预定，无法续借");
        }
        if (!record.renew(RENEWAL_EXTENSION_DAYS, MAX_RENEWALS)) {
            throw new BusinessException("已达到最大续借次数(" + MAX_RENEWALS + "次)");
        }
        borrowRecordDao.update(record);
        LOG.info("续借: 记录ID=" + recordId + " 新归还日期=" + record.getDueDate());
        return record;
    }

    public long calculateFine(BorrowRecord record) {
        if (record == null || record.getDueDate() == null) return 0;
        LocalDate endDate = record.getReturnDate() != null ? record.getReturnDate() : LocalDate.now();
        long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), endDate);
        return overdueDays > 0 ? overdueDays * FINE_PER_DAY : 0;
    }

    public List<BorrowRecord> getUserRecords(Long userId) {
        return borrowRecordDao.findByUserId(userId);
    }

    public List<BorrowRecord> getUnreturnedRecords(Long userId) {
        return borrowRecordDao.findUnreturnedByUserId(userId);
    }

    public int sendOverdueReminders() {
        List<BorrowRecord> overdueRecords = borrowRecordDao.findOverdueRecords();
        int count = 0;
        for (BorrowRecord record : overdueRecords) {
            if (notificationService != null) {
                Book book = bookDao.findById(record.getBookId());
                String title = book != null ? book.getTitle() : "某本书";
                notificationService.send(record.getUserId(), "您的图书《" + title +
                        "》已逾期，请尽快归还并处理罚金。", Notification.Type.OVERDUE_REMINDER);
                count++;
            }
        }
        LOG.info("发送逾期催还提醒: " + count + " 条");
        return count;
    }

    /**
     * 发送长期未还提醒（借阅超过 maxDays 天未归还）。
     * 同时提醒即将到期（borrowDays ~ maxDays 之间）的用户。
     *
     * @param maxDays       长期借阅阈值（如 90 天 = 约3个月）
     * @param warningDays   警告提前天数（如 15 天 → 借阅75天以上开始提醒）
     * @return [已超期提醒数, 即将到期提醒数]
     */
    public int[] sendLongTermReminders(int maxDays, int warningDays) {
        List<BorrowRecord> longTerm = borrowRecordDao.findLongTermBorrowed(maxDays);
        List<BorrowRecord> allUnreturned = borrowRecordDao.findLongTermBorrowed(maxDays - warningDays);
        // 排除已经≥maxDays的，剩下的是接近的
        List<BorrowRecord> approaching = new ArrayList<>(allUnreturned);
        approaching.removeAll(longTerm);

        int overdueCount = 0, warningCount = 0;

        // 已超期：借阅超过 maxDays 天
        for (BorrowRecord record : longTerm) {
            if (notificationService != null) {
                Book book = bookDao.findById(record.getBookId());
                String title = book != null ? book.getTitle() : "某本书";
                long borrowedDays = java.time.temporal.ChronoUnit.DAYS.between(record.getBorrowDate(), java.time.LocalDate.now());
                notificationService.send(record.getUserId(),
                        "【长期未还】您的图书《" + title + "》已借阅 " + borrowedDays
                        + " 天（超过" + (maxDays / 30) + "个月），请尽快归还！",
                        Notification.Type.OVERDUE_REMINDER);
                overdueCount++;
            }
        }

        // 即将到期：借阅在 (maxDays - warningDays) ~ maxDays 之间
        for (BorrowRecord record : approaching) {
            if (notificationService != null) {
                Book book = bookDao.findById(record.getBookId());
                String title = book != null ? book.getTitle() : "某本书";
                long borrowedDays = java.time.temporal.ChronoUnit.DAYS.between(record.getBorrowDate(), java.time.LocalDate.now());
                long remainingDays = maxDays - borrowedDays;
                notificationService.send(record.getUserId(),
                        "【即将到期】您的图书《" + title + "》已借阅 " + borrowedDays
                        + " 天，还有约 " + remainingDays + " 天达到借阅上限（" + (maxDays / 30) + "个月），请合理安排归还。",
                        Notification.Type.OVERDUE_REMINDER);
                warningCount++;
            }
        }

        LOG.info("长期未还提醒: 超期 " + overdueCount + " 条, 即将到期 " + warningCount + " 条");
        return new int[]{overdueCount, warningCount};
    }

    public boolean sendOverdueReminderToUser(Long userId) {
        boolean hasOverdue = borrowRecordDao.hasOverdueRecords(userId);
        if (hasOverdue && notificationService != null) {
            notificationService.send(userId, "您有图书已逾期，请尽快归还。", Notification.Type.OVERDUE_REMINDER);
        }
        return hasOverdue;
    }

    private static String centsToYuan(long cents) {
        return String.format("%.2f", cents / 100.0);
    }

    private static boolean isOperator(User user) {
        return user != null && (user.isAdmin() || user.isLibrarian());
    }

    private void cancelReservationIfBorrowed(Long bookId, Long userId) {
        reservationDao.findActiveByUserAndBook(userId, bookId).ifPresent(r -> {
            r.setStatus(Reservation.Status.CANCELLED);
            reservationDao.update(r);
            LOG.info("用户借阅后自动取消预定: userId=" + userId + " bookId=" + bookId);
        });
    }

    private void notifyNextReservation(Long bookId) {
        List<Reservation> reservations = reservationDao.findActiveByBookId(bookId);
        if (!reservations.isEmpty() && notificationService != null) {
            Reservation next = reservations.get(0);
            Book book = bookDao.findById(bookId);
            String title = book != null ? book.getTitle() : "某本书";
            notificationService.send(next.getUserId(), "您预定的图书《" + title +
                    "》已归还，现在可以借阅了！", Notification.Type.BOOK_AVAILABLE);
            next.setStatus(Reservation.Status.NOTIFIED);
            reservationDao.update(next);
            LOG.info("通知预定用户: userId=" + next.getUserId() + " bookId=" + bookId);
        }
    }
}
