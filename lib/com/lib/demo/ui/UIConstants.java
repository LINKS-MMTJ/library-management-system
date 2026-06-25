package com.lib.demo.ui;

import javax.swing.*;
import java.awt.*;

/**
 * UI 常量集中管理：主题色、字体、面板名称、布局常量。
 * 跨平台字体兼容：优先使用系统默认中文字体，失败则回退到 SANS_SERIF。
 */
final class UIConstants {

    private UIConstants() {}

    // ── 字体（跨平台兼容） ──
    private static final String[] FONT_CANDIDATES = {
        "Microsoft YaHei",   // Windows
        "PingFang SC",       // macOS
        "Noto Sans CJK SC",  // Linux
        "WenQuanYi Micro Hei", // Linux
        "SimHei",            // Windows fallback
    };

    private static String resolveFontName() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String name : FONT_CANDIDATES) {
            for (String available : ge.getAvailableFontFamilyNames()) {
                if (available.equals(name)) return name;
            }
        }
        return Font.SANS_SERIF; // 最终回退
    }

    static final String FONT_FAMILY = resolveFontName();

    static final Font FONT_TITLE  = new Font(FONT_FAMILY, Font.BOLD, 20);
    static final Font FONT_NORMAL = new Font(FONT_FAMILY, Font.PLAIN, 13);
    static final Font FONT_SMALL  = new Font(FONT_FAMILY, Font.PLAIN, 12);
    static final Font FONT_TINY   = new Font(FONT_FAMILY, Font.PLAIN, 10);
    static final Font FONT_MONO   = new Font("Consolas", Font.PLAIN, 12);

    // ── 主题色 ──
    static final Color PRIMARY      = new Color(59, 130, 246);
    static final Color PRIMARY_DARK = new Color(37, 99, 235);
    static final Color SUCCESS      = new Color(16, 185, 129);
    static final Color SUCCESS_DARK = new Color(5, 150, 105);
    static final Color WARNING      = new Color(234, 179, 8);
    static final Color WARNING_BG   = new Color(254, 240, 138);
    static final Color DANGER       = new Color(220, 38, 38);
    static final Color GRAY_BTN     = new Color(71, 85, 105);

    static final Color BG_SIDEBAR   = new Color(30, 41, 59);
    static final Color BG_MAIN      = new Color(241, 245, 249);
    static final Color BG_WHITE     = Color.WHITE;
    static final Color TEXT_WHITE   = new Color(255, 255, 255);
    static final Color TEXT_GRAY    = new Color(100, 116, 139);
    static final Color TEXT_DARK    = new Color(30, 41, 59);
    static final Color TEXT_SIDEBAR = new Color(226, 232, 240);
    static final Color TEXT_SECTION = new Color(170, 180, 200);
    static final Color NAV_ACTIVE_BG = new Color(51, 65, 85);
    static final Color TABLE_STRIPE = new Color(248, 250, 252);
    static final Color TABLE_HOVER  = new Color(238, 242, 255);
    static final Color BORDER_LIGHT = new Color(226, 232, 240);

    // ── 面板名称常量 ──
    static final String VIEW_DASHBOARD     = "dashboard";
    static final String VIEW_BOOKS         = "books";
    static final String VIEW_USERS         = "users";
    static final String VIEW_MY_BORROWS    = "myBorrows";
    static final String VIEW_BORROW_MANAGE = "borrowManage";
    static final String VIEW_RESERVATIONS  = "reservations";
    static final String VIEW_NOTIFICATIONS = "notifications";
    static final String VIEW_SYSTEM        = "system";

    static final String CARD_LOGIN = "LOGIN";
    static final String CARD_APP   = "APP";

    // ── 导航标题映射 ──
    static final String[] NAV_VIEWS  = {"dashboard", "books", "users", "myBorrows", "borrowManage", "reservations", "notifications", "system"};
    static final String[] NAV_TITLES = {"仪表盘", "图书列表", "用户列表", "我的借阅", "借阅管理", "预约管理", "消息通知", "系统操作"};
}
