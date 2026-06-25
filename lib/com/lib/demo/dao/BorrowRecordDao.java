package com.lib.demo.dao;

import com.lib.demo.entity.BorrowRecord;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 借阅记录数据访问对象。
 */
public class BorrowRecordDao extends AbstractDao<BorrowRecord> {

    public BorrowRecordDao() {
        super(DataFileNames.BORROW_RECORDS);
    }

    @Override
    protected Long getId(BorrowRecord entity) { return entity.getRecordId(); }

    @Override
    protected void setId(BorrowRecord entity, Long id) { entity.setRecordId(id); }

    @Override
    protected String entityName() { return "借阅记录"; }

    public synchronized List<BorrowRecord> findByUserId(Long userId) {
        return dataMap.values().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted((r1, r2) -> r2.getBorrowDate().compareTo(r1.getBorrowDate()))
                .collect(Collectors.toList());
    }

    public synchronized List<BorrowRecord> findUnreturnedByUserId(Long userId) {
        return dataMap.values().stream()
                .filter(r -> r.getUserId().equals(userId) && !r.isReturned())
                .sorted((r1, r2) -> r2.getBorrowDate().compareTo(r1.getBorrowDate()))
                .collect(Collectors.toList());
    }

    public synchronized boolean hasOverdueRecords(Long userId) {
        return dataMap.values().stream()
                .anyMatch(r -> r.getUserId().equals(userId)
                        && r.getStatus() == BorrowRecord.Status.BORROWED
                        && r.isOverdue());
    }

    public synchronized List<BorrowRecord> findOverdueRecords() {
        return dataMap.values().stream()
                .filter(r -> r.getStatus() == BorrowRecord.Status.BORROWED && r.isOverdue())
                .collect(Collectors.toList());
    }

    /** 查找借阅天数 ≥ days 且未归还的记录 */
    public synchronized List<BorrowRecord> findLongTermBorrowed(int days) {
        java.time.LocalDate threshold = java.time.LocalDate.now().minusDays(days);
        return dataMap.values().stream()
                .filter(r -> !r.isReturned()
                        && r.getStatus() == BorrowRecord.Status.BORROWED
                        && !r.getBorrowDate().isAfter(threshold))
                .sorted(Comparator.comparing(BorrowRecord::getBorrowDate))
                .collect(Collectors.toList());
    }
}
