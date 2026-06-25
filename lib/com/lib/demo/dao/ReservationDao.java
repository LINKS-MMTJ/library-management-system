package com.lib.demo.dao;

import com.lib.demo.entity.Reservation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 预约数据访问对象。
 */
public class ReservationDao extends AbstractDao<Reservation> {

    public ReservationDao() {
        super(DataFileNames.RESERVATIONS);
    }

    @Override
    protected Long getId(Reservation entity) { return entity.getReservationId(); }

    @Override
    protected void setId(Reservation entity, Long id) { entity.setReservationId(id); }

    @Override
    protected String entityName() { return "预约"; }

    public synchronized List<Reservation> findActiveByBookId(Long bookId) {
        return dataMap.values().stream()
                .filter(r -> r.getBookId().equals(bookId) && r.getStatus() == Reservation.Status.ACTIVE)
                .sorted(Comparator.comparing(Reservation::getRequestDate))
                .collect(Collectors.toList());
    }

    public synchronized boolean hasActiveReservation(Long bookId, Long excludeUserId) {
        return dataMap.values().stream()
                .anyMatch(r -> r.getBookId().equals(bookId)
                        && (r.getStatus() == Reservation.Status.ACTIVE
                            || r.getStatus() == Reservation.Status.NOTIFIED)
                        && !r.getUserId().equals(excludeUserId));
    }

    public synchronized Optional<Reservation> findActiveByUserAndBook(Long userId, Long bookId) {
        return dataMap.values().stream()
                .filter(r -> r.getUserId().equals(userId)
                        && r.getBookId().equals(bookId)
                        && r.getStatus() == Reservation.Status.ACTIVE)
                .findFirst();
    }

    public synchronized boolean hasActiveReservation(Long bookId) {
        return dataMap.values().stream()
                .anyMatch(r -> r.getBookId().equals(bookId)
                        && (r.getStatus() == Reservation.Status.ACTIVE
                            || r.getStatus() == Reservation.Status.NOTIFIED));
    }

    public synchronized List<Reservation> findLongWaiting(int maxWaitDays) {
        return dataMap.values().stream()
                .filter(r -> r.getStatus() == Reservation.Status.ACTIVE
                        && r.getRequestDate().isBefore(java.time.LocalDate.now().minusDays(maxWaitDays)))
                .collect(Collectors.toList());
    }
}
