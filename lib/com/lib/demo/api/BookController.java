package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.Book;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;

import java.time.LocalDate;
import java.util.*;

/**
 * 图书控制器 — 图书 CRUD + 借阅/预约操作。
 */
class BookController {
    private final AppContext ctx;

    BookController(AppContext ctx) { this.ctx = ctx; }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        Long id = ApiServer.parseIdParam(parts, 1);
        String action = parts.length > 2 ? parts[2] : (id != null && method.equals("POST") ? parts[1] : "");

        // 路由: /api/books, /api/books/:id, /api/books/:id/borrow, /api/books/:id/reserve
        if (parts.length >= 3 && id != null) {
            switch (parts[2]) {
                case "borrow": return borrowBook(id, user);
                case "reserve": return reserveBook(id, user);
            }
        }

        if (id != null) {
            switch (method) {
                case "GET": return getBook(id);
                case "PUT": return updateBook(id, body, user);
                case "DELETE": return removeBook(id, body, user);
                default: throw new ApiServer.BusinessHttpException(405, "不支持的请求方法");
            }
        } else {
            switch (method) {
                case "GET": return listBooks(body.get("keyword"));
                case "POST": return addBook(body, user);
                default: throw new ApiServer.BusinessHttpException(405, "不支持的请求方法");
            }
        }
    }

    private String listBooks(String keyword) {
        List<Book> books = (keyword != null && !keyword.trim().isEmpty())
                ? ctx.getBookService().searchBooks(keyword)
                : ctx.getBookService().getAllBooks();
        return JsonUtil.listJson(books);
    }

    private String getBook(Long id) {
        return JsonUtil.successJson(JsonUtil.toJson(ctx.getBookService().getBookById(id)));
    }

    private String addBook(Map<String, String> body, User user) {
        ApiServer.requireAdmin(user);
        Book book = new Book();
        book.setIsbn(body.get("isbn"));
        book.setTitle(body.get("title"));
        book.setAuthor(get(body, "author"));
        book.setPublisher(get(body, "publisher"));
        book.setCategory(get(body, "category"));
        book.setLocation(get(body, "location"));
        String dateStr = body.get("publishDate");
        if (dateStr != null && !dateStr.isEmpty()) book.setPublishDate(LocalDate.parse(dateStr));
        int qty = JsonUtil.getInt(body, "quantity", 1);
        Book result = ctx.getBookService().addBook(book, qty, user);
        return JsonUtil.successJson(JsonUtil.toJson(result));
    }

    private String updateBook(Long id, Map<String, String> body, User user) {
        ApiServer.requireAdmin(user);
        Book update = new Book();
        update.setTitle(get(body, "title"));
        update.setAuthor(get(body, "author"));
        update.setPublisher(get(body, "publisher"));
        update.setCategory(get(body, "category"));
        update.setLocation(get(body, "location"));
        String dateStr = body.get("publishDate");
        if (dateStr != null && !dateStr.isEmpty()) update.setPublishDate(LocalDate.parse(dateStr));
        Book result = ctx.getBookService().updateBookInfo(id, update, user);
        return JsonUtil.successJson(JsonUtil.toJson(result));
    }

    private String removeBook(Long id, Map<String, String> body, User user) {
        ApiServer.requireAdmin(user);
        int qty = JsonUtil.getInt(body, "quantity", 1);
        String reason = body.getOrDefault("reason", "");
        ctx.getBookService().removeBook(id, qty, reason, user);
        return JsonUtil.successMsg("下架成功");
    }

    private String borrowBook(Long id, User user) {
        ApiServer.requireAuth(user);
        ctx.getBorrowService().borrowBook(user.getUserId(), id, user);
        return JsonUtil.successMsg("借阅成功");
    }

    private String reserveBook(Long id, User user) {
        ApiServer.requireAuth(user);
        ctx.getReservationService().reserveBook(user.getUserId(), id);
        return JsonUtil.successMsg("预约成功");
    }

    private String get(Map<String, String> body, String key) {
        String v = body.get(key);
        return (v == null || v.isEmpty()) ? null : v;
    }
}
