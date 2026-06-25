package com.lib.demo.dao;

/**
 * 集中管理数据文件名常量，避免硬编码分散在各 DAO 中。
 */
final class DataFileNames {
    private DataFileNames() {}

    static final String BOOKS = "books.dat";
    static final String USERS = "users.dat";
    static final String BORROW_RECORDS = "borrow_records.dat";
    static final String RESERVATIONS = "reservations.dat";
    static final String NOTIFICATIONS = "notifications.dat";
}
