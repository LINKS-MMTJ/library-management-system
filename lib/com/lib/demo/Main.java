package com.lib.demo;

import com.lib.demo.api.ApiServer;

/**
 * 图书管理系统 — REST API + Vue3 前端
 * 默认测试账号：admin/admin123 | lib1/lib123 | user1/user123
 */
public class Main {
    public static void main(String[] args) {
        int port = AppConfig.getServerPort(args);
        try {
            AppContext ctx = AppContext.create();
            ApiServer server = new ApiServer(ctx, port);
            server.start();
            System.out.println("===================================");
            System.out.println("  图书管理系统 API 已启动");
            System.out.println("  http://localhost:" + port);
            System.out.println("  http://localhost:" + port + "/api/   (REST API)");
            System.out.println("===================================");
        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
