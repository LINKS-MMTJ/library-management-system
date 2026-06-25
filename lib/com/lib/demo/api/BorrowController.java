package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.Book;
import com.lib.demo.entity.BorrowRecord;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 借阅控制器 — 我的借阅/全部借阅/归还/续借。
 */
class BorrowController {
    private final AppContext ctx;

    BorrowController(AppContext ctx) { this.ctx = ctx; }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        Long id = ApiServer.parseIdParam(parts, 1);

        if (parts.length >= 3 && id != null) {
            switch (parts[2]) {
                case "return": return returnBook(id, user);
                case "renew":  return renewBook(id, user);
            }
        }

        if ("my".equals(parts.length > 1 ? parts[1] : "")) {
            return myBorrows(user);
        }

        return listAll(user);
    }

    private String listAll(User user) {
        ApiServer.requireAdmin(user);
        List<BorrowRecord> records = ctx.getBorrowRecordDao().findAll();
        return JsonUtil.listJson(enrichRecords(records));
    }

    private String myBorrows(User user) {
        ApiServer.requireAuth(user);
        List<BorrowRecord> records = ctx.getBorrowService().getUserRecords(user.getUserId());
        return JsonUtil.listJson(enrichRecords(records));
    }

    private String returnBook(Long id, User user) {
        ApiServer.requireAuth(user);
        ctx.getBorrowService().returnBookByRecordId(id, user);
        return JsonUtil.successMsg("归还成功");
    }

    private String renewBook(Long id, User user) {
        ApiServer.requireAuth(user);
        BorrowRecord r = ctx.getBorrowService().renewBook(id, user);
        return JsonUtil.successJson(JsonUtil.toJson(r));
    }

    /** 为借阅记录附加图书名和用户名 */
    private List<Map<String, Object>> enrichRecords(List<BorrowRecord> records) {
        return records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", r.getRecordId());
            m.put("userId", r.getUserId());
            m.put("bookId", r.getBookId());
            Book book = ctx.getBookDao().findById(r.getBookId());
            m.put("bookTitle", book != null ? book.getTitle() : "未知");
            User u = ctx.getUserDao().findById(r.getUserId());
            m.put("userName", u != null ? u.getName() : "未知");
            m.put("borrowDate", r.getBorrowDate().toString());
            m.put("dueDate", r.getDueDate().toString());
            m.put("returnDate", r.getReturnDate() != null ? r.getReturnDate().toString() : null);
            m.put("status", r.getStatus().name());
            m.put("statusDesc", r.isReturned() ? "已归还" : (r.isOverdue() ? "已逾期" : "借阅中"));
            m.put("fineAmount", r.getFineAmount());
            m.put("renewCount", r.getRenewCount());
            return m;
        }).collect(Collectors.toList());
    }
}
