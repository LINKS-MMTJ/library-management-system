package com.lib.demo.api;

import com.lib.demo.AppConfig;
import com.lib.demo.AppContext;
import com.lib.demo.entity.User;
import com.lib.demo.util.JsonUtil;
import com.lib.demo.util.LogUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST API 服务器 — 基于 JDK 内置 HttpServer，零外部依赖。
 */
public class ApiServer {
    private static final Logger LOG = LogUtil.getLogger(ApiServer.class);
    private final HttpServer server;
    private final AppContext ctx;

    /** Token 有效期（毫秒），通过 AppConfig 配置，默认 24 小时 */
    private static final long TOKEN_TTL_MS = AppConfig.getTokenTtlMs();

    /** 会话信息 */
    static class SessionInfo {
        final User user;
        final long expireAt;
        SessionInfo(User user) { this.user = user; this.expireAt = System.currentTimeMillis() + TOKEN_TTL_MS; }
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    // Token 会话管理（每 30 分钟清理过期 session）
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    {
        java.util.Timer timer = new java.util.Timer(true);
        timer.schedule(new java.util.TimerTask() {
            @Override public void run() {
                sessions.entrySet().removeIf(e -> e.getValue().isExpired());
            }
        }, TOKEN_TTL_MS, 30 * 60 * 1000L);
    }

    // 子控制器
    private final AuthController authCtrl;
    private final BookController bookCtrl;
    private final UserController userCtrl;
    private final BorrowController borrowCtrl;
    private final ReservationController reservationCtrl;
    private final NotificationController notifCtrl;
    private final SystemController systemCtrl;

    @SuppressWarnings("this-escape")
    public ApiServer(AppContext ctx, int port) throws IOException {
        this.ctx = ctx;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.authCtrl = new AuthController(ctx, sessions);
        this.bookCtrl = new BookController(ctx);
        this.userCtrl = new UserController(ctx);
        this.borrowCtrl = new BorrowController(ctx);
        this.reservationCtrl = new ReservationController(ctx);
        this.notifCtrl = new NotificationController(ctx);
        this.systemCtrl = new SystemController(ctx);

        // 注册路由
        server.createContext("/api/", this::handleApi);
        // 静态文件服务（前端构建产物）
        String staticDir = findStaticDir();
        if (staticDir != null) {
            LOG.info("静态文件目录: " + staticDir);
            server.createContext("/", new StaticFileHandler(staticDir));
        }

        server.setExecutor(null); // 使用默认 executor
    }

    private static String findStaticDir() {
        String[] candidates = {"frontend/dist", "../frontend/dist", "lib2/frontend/dist"};
        for (String dir : candidates) {
            Path p = Paths.get(dir);
            if (Files.isDirectory(p) && Files.exists(p.resolve("index.html"))) {
                return p.toAbsolutePath().toString();
            }
        }
        return null;
    }

    public void start() {
        server.start();
        LOG.info("API 服务器已启动: http://localhost:" + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }

    // ── API 路由分发 ──
    private void handleApi(HttpExchange ex) throws IOException {
        try {
            addCors(ex);
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                sendEmpty(ex, 204);
                return;
            }

            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod().toUpperCase();
            Map<String, String> body = new LinkedHashMap<>();

            // 解析 URL query 参数（如 ?keyword=xxx）
            String query = ex.getRequestURI().getQuery();
            if (query != null) {
                body.putAll(parseQuery(query));
            }

            // 解析请求体（POST/PUT/DELETE，合并到同一 Map）
            if (needsBody(method)) {
                Map<String, String> parsed = readBody(ex);
                if (parsed != null) body.putAll(parsed);
            }

            User authUser = getAuthUser(ex);

            String json;
            try {
                json = dispatch(path, method, body, authUser);
            } catch (BusinessHttpException e) {
                json = JsonUtil.errorJson(e.getCode(), e.getMessage());
            }

            sendJson(ex, 200, json);
        } catch (Exception e) {
            LOG.warning("API 异常: " + e.getMessage());
            sendJson(ex, 500, JsonUtil.errorJson(500, "服务器内部错误: " + e.getMessage()));
        }
    }

    /** 路由分发 */
    private String dispatch(String path, String method, Map<String, String> body, User user) {
        // 提取路径后缀: /api/books/123 → books/123
        String route = path.replaceFirst("^/api/?", "");
        if (route.isEmpty()) {
            return "{\"code\":200,\"data\":{\"name\":\"图书管理系统 API\",\"version\":\"2.0\",\"endpoints\":[\"/api/auth/login\",\"/api/auth/register\",\"/api/books\",\"/api/users\",\"/api/borrows\",\"/api/reservations\",\"/api/notifications\",\"/api/system\",\"/api/dashboard\"]}}";
        }
        String[] parts = route.split("/");

        switch (parts[0]) {
            case "auth":     return authCtrl.handle(parts, method, body, user);
            case "books":    return bookCtrl.handle(parts, method, body, user);
            case "users":    return userCtrl.handle(parts, method, body, user);
            case "borrows":  return borrowCtrl.handle(parts, method, body, user);
            case "reservations": return reservationCtrl.handle(parts, method, body, user);
            case "notifications": return notifCtrl.handle(parts, method, body, user);
            case "system":   return systemCtrl.handle(parts, method, body, user);
            case "dashboard": return handleDashboard(user);
            default:
                throw new BusinessHttpException(404, "接口不存在: " + path);
        }
    }

    private String handleDashboard(User user) {
        requireAuth(user);
        Map<String, Object> stats = new LinkedHashMap<>();
        var books = ctx.getBookService().getAllBooks();
        stats.put("totalBooks", books.size());
        stats.put("totalCopies", books.stream().mapToInt(b -> b.getTotalCopies() != null ? b.getTotalCopies() : 0).sum());
        stats.put("availableCopies", books.stream().mapToInt(b -> b.getAvailableCopies() != null ? b.getAvailableCopies() : 0).sum());
        var allRecords = ctx.getBorrowRecordDao().findAll();
        stats.put("activeBorrows", allRecords.stream().filter(r -> !r.isReturned()).count());
        stats.put("overdueCount", allRecords.stream().filter(r -> r.getStatus() == com.lib.demo.entity.BorrowRecord.Status.BORROWED && r.isOverdue()).count());
        stats.put("activeReservations", ctx.getReservationDao().findAll().stream().filter(r -> r.getStatus() == com.lib.demo.entity.Reservation.Status.ACTIVE).count());
        stats.put("unreadNotifications", ctx.getNotificationService().getUnreadNotifications(user.getUserId()).size());
        List<Map<String, Object>> recent = allRecords.stream()
                .sorted((a, b) -> b.getBorrowDate().compareTo(a.getBorrowDate()))
                .limit(8).map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    var b = ctx.getBookDao().findById(r.getBookId());
                    var u = ctx.getUserDao().findById(r.getUserId());
                    m.put("recordId", r.getRecordId());
                    m.put("bookTitle", b != null ? b.getTitle() : "未知");
                    m.put("userName", u != null ? u.getName() : "未知");
                    m.put("borrowDate", r.getBorrowDate().toString());
                    m.put("dueDate", r.getDueDate().toString());
                    m.put("status", r.isReturned() ? "已归还" : (r.isOverdue() ? "已逾期" : "借阅中"));
                    return m;
                }).collect(Collectors.toList());
        stats.put("recentRecords", recent);
        return "{\"code\":200,\"data\":" + JsonUtil.toJson(stats) + "}";
    }

    // ── Token 认证 ──
    User getAuthUser(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            SessionInfo session = sessions.get(auth.substring(7));
            if (session != null && !session.isExpired()) {
                return session.user;
            }
        }
        return null;
    }

    static void requireAuth(User user) {
        if (user == null) throw new BusinessHttpException(401, "请先登录");
    }

    static void requireAdmin(User user) {
        requireAuth(user);
        if (!user.isAdmin() && !user.isLibrarian()) {
            throw new BusinessHttpException(403, "权限不足，仅管理员/图书管理员可操作");
        }
    }

    static void requireAdminOnly(User user) {
        requireAuth(user);
        if (!user.isAdmin()) throw new BusinessHttpException(403, "权限不足，仅系统管理员可操作");
    }

    // ── HTTP 工具方法 ──

    private static boolean needsBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
    }

    private Map<String, String> readBody(HttpExchange ex) throws IOException {
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        return body.isEmpty() ? Collections.emptyMap() : JsonUtil.parseSimple(body);
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        addCors(ex);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void sendEmpty(HttpExchange ex, int code) throws IOException {
        addCors(ex);
        ex.sendResponseHeaders(code, -1);
    }

    /** 解析 URL Query 字符串为 Map */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                map.put(pair.substring(0, idx), urlDecode(pair.substring(idx + 1)));
            } else if (!pair.isEmpty()) {
                map.put(pair, "");
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    static void addCors(HttpExchange ex) {
        // 开发模式允许任意来源访问；生产部署时应限制为具体域名
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // ── 自定义 HTTP 业务异常 ──
    static class BusinessHttpException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int code;
        BusinessHttpException(int code, String msg) { super(msg); this.code = code; }
        int getCode() { return code; }
    }

    // ── 静态文件处理器 ──
    static class StaticFileHandler implements HttpHandler {
        private final String rootDir;
        StaticFileHandler(String rootDir) { this.rootDir = rootDir; }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String reqPath = ex.getRequestURI().getPath();
            if (reqPath.equals("/")) reqPath = "/index.html";
            // SPA fallback: 非 API 且非静态文件 → index.html
            Path file = Paths.get(rootDir, reqPath);
            if (!Files.exists(file) || Files.isDirectory(file)) {
                file = Paths.get(rootDir, "index.html");
            }
            String contentType = guessContentType(reqPath);
            ex.getResponseHeaders().set("Content-Type", contentType);
            byte[] bytes = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
            if (reqPath.endsWith(".html")) {
                ex.getResponseHeaders().set("Cache-Control", "no-cache");
            }
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }

        private String guessContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".css")) return "text/css; charset=utf-8";
            if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    static Long parseIdParam(String[] parts, int idx) {
        if (parts.length <= idx) return null;
        try { return Long.parseLong(parts[idx]); } catch (NumberFormatException e) { return null; }
    }
}
