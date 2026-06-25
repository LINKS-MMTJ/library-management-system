package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;

import java.util.Map;

/**
 * 用户管理控制器 — 管理员 CRUD + 禁用/启用/删除。
 */
class UserController {
    private final AppContext ctx;

    UserController(AppContext ctx) { this.ctx = ctx; }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        Long id = ApiServer.parseIdParam(parts, 1);

        if (parts.length >= 3 && id != null) {
            switch (parts[2]) {
                case "disable": return disableUser(id, user);
                case "enable":  return enableUser(id, user);
                case "pay-fine": return payFine(id, body, user);
            }
        }

        if (id != null) {
            switch (method) {
                case "GET": return getUser(id, user);
                case "PUT": return updateUser(id, body, user);
                case "DELETE": return deleteUser(id, user);
                default: throw new ApiServer.BusinessHttpException(405, "不支持的请求方法");
            }
        } else {
            switch (method) {
                case "GET": return listUsers(user);
                case "POST": return createUser(body, user);
                default: throw new ApiServer.BusinessHttpException(405, "不支持的请求方法");
            }
        }
    }

    private String listUsers(User user) {
        ApiServer.requireAdminOnly(user);
        return JsonUtil.listJson(ctx.getUserService().getAllUsers(user));
    }

    private String getUser(Long id, User user) {
        ApiServer.requireAuth(user);
        return JsonUtil.successJson(JsonUtil.toJson(ctx.getUserService().getUserById(id)));
    }

    private String createUser(Map<String, String> body, User user) {
        ApiServer.requireAdminOnly(user);
        User.Role role = parseRole(body.get("role"));
        User result = ctx.getUserService().createUser(
                body.get("username"), body.get("password"), body.get("name"),
                role, User.Status.ACTIVE,
                get(body, "email"), get(body, "phone"), user);
        return JsonUtil.successJson(JsonUtil.toJson(result));
    }

    private String updateUser(Long id, Map<String, String> body, User user) {
        ApiServer.requireAdminOnly(user);
        User update = new User();
        // name: 为空时保留旧值（用 get() 转 null → Service 跳过更新）
        if (body.containsKey("name")) update.setName(get(body, "name"));
        // email/phone: 允许清空（空字符串直接传递）
        if (body.containsKey("email")) update.setEmail(body.get("email"));
        if (body.containsKey("phone")) update.setPhone(body.get("phone"));
        if (body.containsKey("role")) update.setRole(parseRole(body.get("role")));
        if (body.containsKey("status")) update.setStatus("INACTIVE".equals(body.get("status")) ? User.Status.INACTIVE : User.Status.ACTIVE);
        if (body.containsKey("password")) update.setPassword(body.get("password"));
        User result = ctx.getUserService().updateUser(id, update, user);
        return JsonUtil.successJson(JsonUtil.toJson(result));
    }

    private String deleteUser(Long id, User user) {
        ApiServer.requireAdminOnly(user);
        ctx.getUserService().deleteUser(id, user);
        return JsonUtil.successMsg("删除成功");
    }

    private String disableUser(Long id, User user) {
        ApiServer.requireAdminOnly(user);
        ctx.getUserService().disableUser(id, user);
        return JsonUtil.successMsg("已禁用");
    }

    private String enableUser(Long id, User user) {
        ApiServer.requireAdminOnly(user);
        ctx.getUserService().enableUser(id, user);
        return JsonUtil.successMsg("已启用");
    }

    private String payFine(Long id, Map<String, String> body, User user) {
        ApiServer.requireAuth(user);
        long amountCents = JsonUtil.getLong(body, "amount", 0);  // 前端传的是分
        ctx.getUserService().payFine(id, amountCents);
        User updated = ctx.getUserDao().findById(id);
        return JsonUtil.successJson(JsonUtil.toJson(updated));
    }

    private static User.Role parseRole(String role) {
        if ("ADMIN".equals(role)) return User.Role.ADMIN;
        if ("LIBRARIAN".equals(role)) return User.Role.LIBRARIAN;
        return User.Role.BORROWER;
    }

    private static String get(Map<String, String> body, String key) {
        String v = body.get(key);
        return (v == null || v.isEmpty()) ? null : v;
    }
}
