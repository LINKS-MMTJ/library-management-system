package com.lib.demo.dao;

import com.lib.demo.entity.Book;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 图书数据访问对象。
 */
public class BookDao extends AbstractDao<Book> {

    public BookDao() {
        super(DataFileNames.BOOKS);
    }

    @Override
    protected Long getId(Book entity) { return entity.getBookId(); }

    @Override
    protected void setId(Book entity, Long id) { entity.setBookId(id); }

    @Override
    protected String entityName() { return "图书"; }

    /** 序列化兼容：初始化时修复 null 字段 */
    @Override
    public synchronized Book save(Book book) {
        if (book.getTotalCopies() == null) book.setTotalCopies(0);
        if (book.getAvailableCopies() == null) book.setAvailableCopies(0);
        return super.save(book);
    }

    public synchronized Optional<Book> findByIsbn(String isbn) {
        return dataMap.values().stream()
                .filter(b -> b.getIsbn().equals(isbn))
                .findFirst();
    }

    public synchronized List<Book> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String lower = keyword.toLowerCase();
        return dataMap.values().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(lower)
                        || b.getAuthor().toLowerCase().contains(lower)
                        || b.getIsbn().contains(keyword))
                .collect(Collectors.toList());
    }
}
