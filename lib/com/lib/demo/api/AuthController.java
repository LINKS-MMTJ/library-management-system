package com.lib.demo.api;

import com.lib.demo.AppContext;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;

import java.util.*;

/**
 * 认证控制器 — 登录/注册/获取当前用户。
 */
class AuthController {
    private final AppContext ctx;
    private final Map<String, User> sessions;

    AuthController(AppContext ctx, Map<String, User> sessions) {
        this.ctx = ctx;
        this.sessions = sessions;
    }

    String handle(String[] parts, String method, Map<String, String> body, User user) {
        String action = parts.length > 1 ? parts[1] : "";
        switch (action) {
            case "login":    return login(body);
            case "register": return register(body);
            case "me":       return getMe(user);
            default: throw new ApiServer.BusinessHttpException(404, "未知认证操作: " + action);
        }
    }

    private String login(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) throw new ApiServer.BusinessHttpException(400, "用户名和密码必填");
        User user = ctx.getUserService().login(username, password);
        if (user == null) throw new ApiServer.BusinessHttpException(401, "用户名或密码错误");
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);
        return JsonUtil.successJson(JsonUtil.toJson(result));
    }

    private String register(Map<String, String> body) {
        String username = body.get("username"), password = body.get("password"),
               name = body.get("name"), email = body.get("email"), phone = body.get("phone");
        User user = ctx.getUserService().register(username, password, name,
                isEmpty(email) ? null : email, isEmpty(phone) ? null : phone);
        return JsonUtil.successJson(JsonUtil.toJson(user));
    }

    private String getMe(User user) {
        ApiServer.requireAuth(user);
        return JsonUtil.successJson(JsonUtil.toJson(user));
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
}
