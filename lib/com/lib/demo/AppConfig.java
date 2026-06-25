package com.lib.demo;

/**
 * 应用配置 — 所有可调参数集中管理。
 * 支持通过系统属性或命令行参数覆盖默认值。
 *
 * <pre>
 *   java -Dserver.port=9090 -Ddata.dir=/var/lib/data com.lib.demo.Main
 * </pre>
 */
public final class AppConfig {

    private AppConfig() {}

    /** API 服务器端口（默认 8080，可通过 -Dserver.port=9090 或命令行参数覆盖） */
    public static int getServerPort(String[] args) {
        // 优先系统属性
        String prop = System.getProperty("server.port");
        if (prop != null && !prop.isEmpty()) {
            try { return Integer.parseInt(prop); } catch (NumberFormatException ignored) {}
        }
        // 其次命令行参数
        if (args != null && args.length > 0) {
            try { return Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }

    /** 数据文件存储目录（默认 "library_data"，可通过 -Ddata.dir 覆盖） */
    public static String getDataDir() {
        String prop = System.getProperty("data.dir");
        return (prop != null && !prop.isEmpty()) ? prop : "library_data";
    }

    /** Token 有效期（毫秒），默认 24 小时 */
    public static long getTokenTtlMs() {
        String prop = System.getProperty("token.ttl");
        if (prop != null && !prop.isEmpty()) {
            try { return Long.parseLong(prop); } catch (NumberFormatException ignored) {}
        }
        return 24 * 60 * 60 * 1000L;
    }
}
