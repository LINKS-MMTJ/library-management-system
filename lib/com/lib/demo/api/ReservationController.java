package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.Book;
import com.lib.demo.entity.Reservation;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约控制器 — 列表/取消预约。
 */
class ReservationController {
    private final AppContext ctx;

    ReservationController(AppContext ctx) { this.ctx = ctx; }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        Long id = ApiServer.parseIdParam(parts, 1);

        if (parts.length >= 3 && id != null && "cancel".equals(parts[2])) {
            return cancelReservation(id, user);
        }

        return listReservations(user);
    }

    private String listReservations(User user) {
        ApiServer.requireAuth(user);
        List<Reservation> list;
        if (user.isAdmin() || user.isLibrarian()) {
            list = ctx.getReservationDao().findAll();
        } else {
            list = ctx.getReservationDao().findAll().stream()
                    .filter(r -> r.getUserId().equals(user.getUserId()))
                    .collect(Collectors.toList());
        }
        List<Map<String, Object>> enriched = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reservationId", r.getReservationId());
            m.put("userId", r.getUserId());
            m.put("bookId", r.getBookId());
            Book book = ctx.getBookDao().findById(r.getBookId());
            m.put("bookTitle", book != null ? book.getTitle() : "未知");
            m.put("bookAuthor", book != null ? book.getAuthor() : "未知");
            User u = ctx.getUserDao().findById(r.getUserId());
            m.put("userName", u != null ? u.getName() : "未知");
            m.put("requestDate", r.getRequestDate().toString());
            m.put("status", r.getStatus().name());
            m.put("statusDesc", r.getStatus().getDescription());
            return m;
        }).collect(Collectors.toList());
        return JsonUtil.listJson(new ArrayList<>(enriched));
    }

    private String cancelReservation(Long id, User user) {
        ApiServer.requireAuth(user);
        ctx.getReservationService().cancelReservation(id, user.getUserId(), user);
        return JsonUtil.successMsg("预约已取消");
    }
}
