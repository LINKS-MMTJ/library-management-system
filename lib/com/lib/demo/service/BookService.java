package com.lib.demo.service;

import com.lib.demo.dao.BookDao;
import com.lib.demo.dao.BorrowRecordDao;
import com.lib.demo.entity.Book;
import com.lib.demo.entity.User;
import com.lib.demo.exception.BusinessException;
import com.lib.demo.util.LogUtil;

import java.util.List;
import java.util.logging.Logger;

public class BookService {
    private static final Logger LOG = LogUtil.getLogger(BookService.class);
    private final BookDao bookDao;
    private final BorrowRecordDao borrowRecordDao;

    public BookService(BookDao bookDao, BorrowRecordDao borrowRecordDao) {
        this.bookDao = bookDao;
        this.borrowRecordDao = borrowRecordDao;
    }

    public Book addBook(Book book, int quantity, User operator) {
        if (!isOperator(operator)) {
            throw new BusinessException("权限不足");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new BusinessException("ISBN不能为空");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new BusinessException("书名不能为空");
        }
        if (quantity <= 0) {
            throw new BusinessException("入库数量必须大于0");
        }

        java.util.Optional<Book> existing = bookDao.findByIsbn(book.getIsbn());
        if (existing.isPresent()) {
            Book exBook = existing.get();
            exBook.setTotalCopies(exBook.getTotalCopies() + quantity);
            exBook.setAvailableCopies(exBook.getAvailableCopies() + quantity);
            bookDao.update(exBook);
            LOG.info("图书已存在，增加库存: " + exBook.getTitle() + " +" + quantity + "本，操作员: " + operator.getUsername());
            return exBook;
        }

        book.setTotalCopies(quantity);
        book.setAvailableCopies(quantity);
        bookDao.save(book);
        LOG.info("新书上架: 《" + book.getTitle() + "》" + quantity + "本，操作员: " + operator.getUsername());
        return book;
    }

    public void removeBook(Long bookId, int quantity, String reason, User operator) {
        if (!isOperator(operator)) {
            throw new BusinessException("权限不足");
        }
        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (quantity <= 0 || quantity > book.getTotalCopies()) {
            throw new BusinessException("出库数量无效");
        }

        // P1-5: 使用 availableCopies 而非 totalCopies - borrowedCount，防止数据不一致时算出负数
        int availableForRemoval = book.getAvailableCopies();
        if (quantity > availableForRemoval) {
            throw new BusinessException("该图书仅剩 " + availableForRemoval + " 本可出库，无法出库 " + quantity + " 本");
        }

        book.setTotalCopies(book.getTotalCopies() - quantity);
        book.setAvailableCopies(book.getAvailableCopies() - quantity);
        if (book.getTotalCopies() == 0) {
            bookDao.delete(bookId);
        } else {
            bookDao.update(book);
        }
        LOG.info("图书出库: ID=" + bookId + " " + quantity + "本，原因: " + reason + "，操作员: " + operator.getUsername());
    }

    public Book updateBookInfo(Long bookId, Book updatedInfo, User operator) {
        if (!isOperator(operator)) {
            throw new BusinessException("权限不足");
        }
        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        book.updateInfo(updatedInfo);
        bookDao.update(book);
        LOG.info("图书信息修改: ID=" + bookId + " 《" + book.getTitle() + "》，操作员: " + operator.getUsername());
        return book;
    }

    public List<Book> searchBooks(String keyword) {
        return bookDao.search(keyword);
    }

    public Book getBookById(Long bookId) {
        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        return book;
    }

    public List<Book> getAllBooks() {
        return bookDao.findAll();
    }

    private static boolean isOperator(User user) {
        return user != null && (user.isAdmin() || user.isLibrarian());
    }
}
