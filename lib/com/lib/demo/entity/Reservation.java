package com.lib.demo.entity;

import java.io.Serializable;
import java.time.LocalDate;

public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        ACTIVE("活跃"),
        CANCELLED("已取消"),
        NOTIFIED("已通知"),
        FULFILLED("已完成");

        private final String description;
        Status(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private Long reservationId;
    private Long userId;
    private Long bookId;
    private LocalDate requestDate;
    private Status status;

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return "Reservation{id=" + reservationId + ", userId=" + userId +
                ", bookId=" + bookId + ", status=" + status + "}";
    }
}
