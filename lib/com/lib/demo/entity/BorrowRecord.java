package com.lib.demo.entity;

import java.io.Serializable;
import java.time.LocalDate;

public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        BORROWED("借阅中"),
        RETURNED("已归还"),
        OVERDUE("已逾期");

        private final String description;
        Status(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private Status status;
    private int renewCount;

    public BorrowRecord() {
        this.status = Status.BORROWED;
        this.renewCount = 0;
    }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getRenewCount() { return renewCount; }
    public void setRenewCount(int renewCount) { this.renewCount = renewCount; }

    public boolean isReturned() { return returnDate != null; }

    public boolean isOverdue() {
        if (isReturned()) return false;
        return LocalDate.now().isAfter(dueDate);
    }

    public void returnBook() {
        this.returnDate = LocalDate.now();
        this.status = Status.RETURNED;
    }

    public boolean renew(int days, int maxRenewals) {
        if (isReturned()) {
            throw new IllegalStateException("已归还的图书无法续借");
        }
        if (this.renewCount >= maxRenewals) {
            return false;
        }
        this.dueDate = this.dueDate.plusDays(days);
        this.renewCount++;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BorrowRecord)) return false;
        BorrowRecord that = (BorrowRecord) o;
        return recordId != null ? recordId.equals(that.recordId) : that.recordId == null;
    }

    @Override
    public int hashCode() {
        return recordId != null ? recordId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BorrowRecord{recordId=" + recordId + ", userId=" + userId +
                ", bookId=" + bookId + ", status=" + status + "}";
    }
}
