package com.lib.demo.service;

import com.lib.demo.dao.BookDao;
import com.lib.demo.dao.ReservationDao;
import com.lib.demo.entity.Book;
import com.lib.demo.entity.Notification;
import com.lib.demo.entity.Reservation;
import com.lib.demo.entity.User;
import com.lib.demo.exception.BusinessException;
import com.lib.demo.util.LogUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

public class ReservationService {
    private static final Logger LOG = LogUtil.getLogger(ReservationService.class);
    private static final int DEFAULT_MAX_WAIT_DAYS = 30;
    private final ReservationDao reservationDao;
    private final BookDao bookDao;
    private NotificationService notificationService;

    public ReservationService(ReservationDao reservationDao, BookDao bookDao) {
        this.reservationDao = reservationDao;
        this.bookDao = bookDao;
    }

    public void setNotificationService(NotificationService ns) {
        this.notificationService = ns;
    }

    public Reservation reserveBook(Long userId, Long bookId) {
        Book book = bookDao.findById(bookId);
        if (book == null) throw new BusinessException("图书不存在");
        if (book.getAvailableCopies() > 0) throw new BusinessException("该图书有可借副本，无需预定");

        java.util.Optional<Reservation> existing = reservationDao.findActiveByUserAndBook(userId, bookId);
        if (existing.isPresent()) throw new BusinessException("您已经预定了此书");

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setBookId(bookId);
        reservation.setRequestDate(LocalDate.now());
        reservation.setStatus(Reservation.Status.ACTIVE);
        reservationDao.save(reservation);

        if (notificationService != null) {
            notificationService.send(userId, "您已成功预定《" + book.getTitle() +
                    "》，待图书归还后将按顺序通知您。", Notification.Type.RESERVATION_SUCCESS);
        }
        LOG.info("预定: userId=" + userId + " bookId=" + bookId + " 《" + book.getTitle() + "》");
        return reservation;
    }

    public void cancelReservation(Long reservationId, Long userId, User operator) {
        Reservation reservation = reservationDao.findById(reservationId);
        if (reservation == null) throw new BusinessException("预定记录不存在");

        boolean isOwner = reservation.getUserId().equals(userId);
        boolean isAdmin = operator != null && (operator.isAdmin() || operator.isLibrarian());
        if (!isOwner && !isAdmin) throw new BusinessException("无权取消此预定");

        if (reservation.getStatus() != Reservation.Status.ACTIVE) {
            throw new BusinessException("该预定已失效");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservationDao.update(reservation);
        // P1-2: 通知下一个排队的人
        List<Reservation> nextInLine = reservationDao.findActiveByBookId(reservation.getBookId());
        if (!nextInLine.isEmpty() && notificationService != null) {
            Reservation next = nextInLine.get(0);
            Book book = bookDao.findById(reservation.getBookId());
            String title = book != null ? book.getTitle() : "某本书";
            notificationService.send(next.getUserId(),
                    "您预定的《" + title + "》排队位置已前移，图书归还后将通知您。",
                    Notification.Type.SYSTEM);
        }
        LOG.info("取消预定: id=" + reservationId + " 操作人=" +
                (operator != null ? operator.getUsername() : "用户" + userId));
    }

    public List<Reservation> getReservationsForBook(Long bookId) {
        return reservationDao.findActiveByBookId(bookId);
    }

    public boolean hasReservation(Long bookId) {
        return reservationDao.hasActiveReservation(bookId);
    }

    public int sendOutOfStockNotifications() {
        return sendOutOfStockNotifications(DEFAULT_MAX_WAIT_DAYS);
    }

    public int sendOutOfStockNotifications(int maxWaitDays) {
        List<Reservation> longWaitings = reservationDao.findLongWaiting(maxWaitDays);
        for (Reservation r : longWaitings) {
            if (notificationService != null) {
                Book book = bookDao.findById(r.getBookId());
                String title = book != null ? book.getTitle() : "某本书";
                notificationService.send(r.getUserId(), "您预定的图书《" + title +
                        "》尚未到货，仍在采购中。", Notification.Type.OUT_OF_STOCK);
            }
        }
        LOG.info("发送缺书通知: " + longWaitings.size() + " 条");
        return longWaitings.size();
    }
}
