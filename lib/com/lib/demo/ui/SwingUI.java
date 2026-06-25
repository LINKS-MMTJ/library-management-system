package com.lib.demo.ui;

import com.lib.demo.AppContext;
import com.lib.demo.entity.*;
import com.lib.demo.exception.BusinessException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.lib.demo.ui.UIConstants.*;
import static com.lib.demo.ui.UIUtils.*;

/**
 * 图书管理系统 Swing GUI —— 主窗口框架 + 导航路由。
 * 面板构建逻辑委托给各子面板，渲染和工具方法提取到 UIUtils/UIConstants。
 */
public class SwingUI {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 跟踪哪些字段有placeholder文本
    private final java.util.Map<JTextField, String> placeholderMap = new java.util.HashMap<>();

    private final AppContext ctx;
    private User currentUser;

    // 窗口组件
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout mainCardLayout;
    private JPanel loginPanel;
    private JPanel appPanel;
    private JPanel navPanel;
    private JPanel contentPanel;
    private CardLayout contentCardLayout;
    private JLabel statusLabel;
    private JLabel userInfoLabel;
    private JLabel roleLabel;
    private JLabel topbarTitle;

    // 各视图面板
    private JPanel dashboardPanel;
    private JPanel booksPanel;
    private JPanel usersPanel;
    private JPanel myBorrowsPanel;
    private JPanel borrowManagePanel;
    private JPanel reservationsPanel;
    private JPanel notificationsPanel;
    private JPanel systemPanel;

    // 导航按钮列表
    private final java.util.List<JButton> navButtons = new ArrayList<>();

    // toast定时器（单实例复用）
    private javax.swing.Timer toastTimer;

    public SwingUI(AppContext ctx) {
        this.ctx = ctx;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        setUIFont(new javax.swing.plaf.FontUIResource(FONT_FAMILY, Font.PLAIN, 13));
        buildFrame();
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    // ==================== 主窗口 ====================

    private void buildFrame() {
        frame = new JFrame("图书管理系统");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);

        mainCardLayout = new CardLayout();
        mainPanel = new JPanel(mainCardLayout);
        mainPanel.add(buildLoginPanel(), CARD_LOGIN);
        mainPanel.add(buildAppLayout(), CARD_APP);
        frame.setContentPane(mainPanel);
    }

    // ==================== 登录面板 ====================

    private JPanel buildLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(30, 41, 59));

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(400, 420));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                new EmptyBorder(30, 30, 30, 30)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("图书管理系统");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("欢迎使用，请登录或注册账号");
        subtitle.setFont(FONT_NORMAL);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(TEXT_GRAY);

        // Tab切换
        JPanel tabPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        tabPanel.setMaximumSize(new Dimension(340, 40));
        tabPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton loginTab = createTabButton("登录", true);
        JButton registerTab = createTabButton("注册", false);

        // 登录表单
        JPanel loginForm = new JPanel();
        loginForm.setLayout(new BoxLayout(loginForm, BoxLayout.Y_AXIS));
        loginForm.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginForm.setMaximumSize(new Dimension(340, 220));
        JTextField loginUsername = new JTextField();
        JPasswordField loginPassword = new JPasswordField();
        styleField(loginUsername, "用户名");
        styleField(loginPassword, "密码");
        loginForm.add(createFieldLabel("用户名"));
        loginForm.add(loginUsername);
        loginForm.add(Box.createVerticalStrut(10));
        loginForm.add(createFieldLabel("密码"));
        loginForm.add(loginPassword);
        loginForm.add(Box.createVerticalStrut(15));

        JButton loginBtn = new JButton("登  录");
        styleButton(loginBtn, PRIMARY, true);
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(340, 38));
        loginForm.add(loginBtn);

        // 注册表单
        JPanel registerForm = new JPanel();
        registerForm.setLayout(new BoxLayout(registerForm, BoxLayout.Y_AXIS));
        registerForm.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerForm.setMaximumSize(new Dimension(340, 360));
        JTextField regUsername = new JTextField();
        JPasswordField regPassword = new JPasswordField();
        JTextField regName = new JTextField();
        JTextField regEmail = new JTextField();
        JTextField regPhone = new JTextField();
        styleField(regUsername, "用户名");
        styleField(regPassword, "密码(至少6位)");
        styleField(regName, "真实姓名");
        styleField(regEmail, "邮箱(选填)");
        styleField(regPhone, "手机号(选填)");
        registerForm.add(createFieldLabel("用户名"));
        registerForm.add(regUsername);
        registerForm.add(Box.createVerticalStrut(6));
        registerForm.add(createFieldLabel("密码"));
        registerForm.add(regPassword);
        registerForm.add(Box.createVerticalStrut(6));
        registerForm.add(createFieldLabel("真实姓名"));
        registerForm.add(regName);
        registerForm.add(Box.createVerticalStrut(6));
        registerForm.add(createFieldLabel("邮箱"));
        registerForm.add(regEmail);
        registerForm.add(Box.createVerticalStrut(6));
        registerForm.add(createFieldLabel("手机号"));
        registerForm.add(regPhone);
        registerForm.add(Box.createVerticalStrut(12));

        JButton regBtn = new JButton("注  册");
        styleButton(regBtn, SUCCESS, true);
        regBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        regBtn.setMaximumSize(new Dimension(340, 38));
        registerForm.add(regBtn);
        registerForm.setVisible(false);

        // Tab切换事件
        loginTab.addActionListener(e -> {
            loginTab.setForeground(PRIMARY);
            registerTab.setForeground(TEXT_GRAY);
            loginTab.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY));
            registerTab.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
            loginForm.setVisible(true);
            registerForm.setVisible(false);
            card.setPreferredSize(new Dimension(400, 420));
            frame.pack(); frame.setSize(1100, 750);
        });
        registerTab.addActionListener(e -> {
            registerTab.setForeground(PRIMARY);
            loginTab.setForeground(TEXT_GRAY);
            registerTab.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY));
            loginTab.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
            registerForm.setVisible(true);
            loginForm.setVisible(false);
            card.setPreferredSize(new Dimension(400, 560));
            frame.pack(); frame.setSize(1100, 750);
        });

        tabPanel.add(loginTab); tabPanel.add(registerTab);

        // 登录事件
        loginBtn.addActionListener(e -> {
            String uname = getRealText(loginUsername);
            String pwd = new String(loginPassword.getPassword());
            if (uname.isEmpty() || pwd.isEmpty()) { showError("用户名和密码不能为空"); return; }
            User user = ctx.getUserService().login(uname, pwd);
            if (user != null) {
                currentUser = user;
                showAppLayout();
                showToast("登录成功！欢迎 " + user.getName());
                loginUsername.setText(""); loginPassword.setText("");
            } else {
                showError("用户名或密码错误，或账户已被禁用");
            }
        });

        // 注册事件
        regBtn.addActionListener(e -> {
            String uname = getRealText(regUsername);
            String pwd = new String(regPassword.getPassword());
            String name = getRealText(regName);
            String email = regEmail.getText().trim();
            String phone = regPhone.getText().trim();
            if (isPlaceholder(regEmail, email)) email = "";
            if (isPlaceholder(regPhone, phone)) phone = "";
            if (uname.isEmpty() || pwd.isEmpty() || name.isEmpty()) { showError("用户名、密码、真实姓名不能为空"); return; }
            if (pwd.length() < 6) { showError("密码至少需要6位"); return; }
            try {
                ctx.getUserService().register(uname, pwd, name,
                        email.isEmpty() ? null : email, phone.isEmpty() ? null : phone);
                showToast("注册成功！请登录");
                loginTab.doClick();
                loginUsername.setText(uname); loginPassword.setText("");
                regUsername.setText(""); regPassword.setText(""); regName.setText("");
                regEmail.setText(""); regPhone.setText("");
            } catch (Exception ex) { showError(ex.getMessage()); }
        });

        loginPassword.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) loginBtn.doClick();
            }
        });

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(18));
        card.add(tabPanel);
        card.add(Box.createVerticalStrut(15));
        card.add(loginForm);
        card.add(registerForm);
        loginPanel.add(card);
        return loginPanel;
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        btn.setForeground(active ? PRIMARY : TEXT_GRAY);
        btn.setBackground(Color.WHITE);
        btn.setBorder(active ? BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY)
                : BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleField(JTextField field, String placeholder) {
        field.setMaximumSize(new Dimension(340, 32));
        field.setFont(FONT_NORMAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        if (!placeholder.isEmpty() && !(field instanceof JPasswordField)) {
            placeholderMap.put(field, placeholder);
            field.setForeground(TEXT_GRAY);
            field.setText(placeholder);
            field.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (field.getText().equals(placeholder)) {
                        field.setText(""); field.setForeground(TEXT_DARK);
                    }
                }
                public void focusLost(FocusEvent e) {
                    if (field.getText().isEmpty()) {
                        field.setText(placeholder); field.setForeground(TEXT_GRAY);
                    }
                }
            });
        }
    }

    private String getRealText(JTextField field) {
        String text = field.getText().trim();
        String placeholder = placeholderMap.get(field);
        return (placeholder != null && placeholder.equals(text)) ? "" : text;
    }

    private boolean isPlaceholder(JTextField field, String value) {
        String placeholder = placeholderMap.get(field);
        return placeholder != null && placeholder.equals(value);
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        label.setForeground(new Color(71, 85, 105));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // ==================== 主应用布局 ====================

    private JPanel buildAppLayout() {
        appPanel = new JPanel(new BorderLayout());

        // 顶部栏
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(10, 20, 10, 20)));
        topBar.setPreferredSize(new Dimension(1100, 50));

        topbarTitle = new JLabel("仪表盘");
        topbarTitle.setFont(FONT_TITLE);
        topbarTitle.setForeground(TEXT_DARK);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        topRight.setBackground(Color.WHITE);
        userInfoLabel = new JLabel();
        userInfoLabel.setFont(FONT_SMALL);
        roleLabel = new JLabel();
        roleLabel.setFont(FONT_TINY);
        roleLabel.setForeground(TEXT_GRAY);

        JButton logoutBtn = new JButton("退出登录");
        logoutBtn.setFont(FONT_SMALL);
        logoutBtn.setForeground(TEXT_GRAY);
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setBorder(new LineBorder(new Color(203, 213, 225), 1, true));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());

        topRight.add(userInfoLabel); topRight.add(roleLabel); topRight.add(logoutBtn);
        topBar.add(topbarTitle, BorderLayout.WEST);
        topBar.add(topRight, BorderLayout.EAST);

        // 侧边导航
        navPanel = new JPanel();
        navPanel.setBackground(BG_SIDEBAR);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setPreferredSize(new Dimension(200, 700));
        navPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        // 内容区域
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setBackground(BG_MAIN);

        dashboardPanel = createDashboardPanel();
        booksPanel = createBooksPanel();
        usersPanel = createUsersPanel();
        myBorrowsPanel = createMyBorrowsPanel();
        borrowManagePanel = createBorrowManagePanel();
        reservationsPanel = createReservationsPanel();
        notificationsPanel = createNotificationsPanel();
        systemPanel = createSystemPanel();

        contentPanel.add(new JScrollPane(dashboardPanel), VIEW_DASHBOARD);
        contentPanel.add(new JScrollPane(booksPanel), VIEW_BOOKS);
        contentPanel.add(new JScrollPane(usersPanel), VIEW_USERS);
        contentPanel.add(new JScrollPane(myBorrowsPanel), VIEW_MY_BORROWS);
        contentPanel.add(new JScrollPane(borrowManagePanel), VIEW_BORROW_MANAGE);
        contentPanel.add(new JScrollPane(reservationsPanel), VIEW_RESERVATIONS);
        contentPanel.add(new JScrollPane(notificationsPanel), VIEW_NOTIFICATIONS);
        contentPanel.add(new JScrollPane(systemPanel), VIEW_SYSTEM);

        // 底部状态栏
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(248, 250, 252));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(4, 16, 4, 16)));
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_GRAY);
        JLabel dateLabel = new JLabel(LocalDate.now().format(FMT));
        dateLabel.setFont(FONT_SMALL);
        dateLabel.setForeground(TEXT_GRAY);
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(dateLabel, BorderLayout.EAST);

        appPanel.add(topBar, BorderLayout.NORTH);
        appPanel.add(navPanel, BorderLayout.WEST);
        appPanel.add(contentPanel, BorderLayout.CENTER);
        appPanel.add(statusBar, BorderLayout.SOUTH);
        return appPanel;
    }

    private void showAppLayout() {
        rebuildNav();
        userInfoLabel.setText("当前用户: " + currentUser.getName());
        roleLabel.setText(currentUser.getRole().getDescription());
        mainCardLayout.show(mainPanel, CARD_APP);
        navigate(VIEW_DASHBOARD);
    }

    private void logout() {
        currentUser = null;
        navPanel.removeAll();
        navPanel.revalidate(); navPanel.repaint();
        mainCardLayout.show(mainPanel, CARD_LOGIN);
    }

    // ==================== 导航 ====================

    private void rebuildNav() {
        navPanel.removeAll();

        JLabel navTitle = new JLabel("  图书管理");
        navTitle.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        navTitle.setForeground(TEXT_WHITE);
        navTitle.setBorder(new EmptyBorder(8, 16, 12, 16));
        navTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        navPanel.add(navTitle);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(51, 65, 85));
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        navPanel.add(sep);
        navPanel.add(Box.createVerticalStrut(8));

        navButtons.clear();

        addNavSection("主菜单");
        addNavButton("仪表盘", VIEW_DASHBOARD);
        addNavSection("图书管理");
        addNavButton("图书列表", VIEW_BOOKS);

        if (currentUser.isAdmin()) {
            addNavSection("用户管理");
            addNavButton("用户列表", VIEW_USERS);
        }

        addNavSection("借阅服务");
        if (currentUser.isAdmin() || currentUser.isLibrarian()) {
            addNavButton("借阅管理", VIEW_BORROW_MANAGE);
        } else {
            addNavButton("我的借阅", VIEW_MY_BORROWS);
        }
        addNavButton("预约管理", VIEW_RESERVATIONS);
        addNavButton("消息通知", VIEW_NOTIFICATIONS);

        if (currentUser.isAdmin()) {
            addNavSection("系统管理");
            addNavButton("系统操作", VIEW_SYSTEM);
        }

        navPanel.add(Box.createVerticalGlue());

        JPanel footerInfo = new JPanel();
        footerInfo.setBackground(BG_SIDEBAR);
        footerInfo.setLayout(new BoxLayout(footerInfo, BoxLayout.Y_AXIS));
        footerInfo.setBorder(new EmptyBorder(10, 16, 10, 16));
        footerInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel footerUser = new JLabel(currentUser.getName());
        footerUser.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        footerUser.setForeground(TEXT_WHITE);
        JLabel footerRole = new JLabel(currentUser.getRole().getDescription());
        footerRole.setFont(FONT_SMALL);
        footerRole.setForeground(new Color(148, 163, 184));
        footerInfo.add(footerUser); footerInfo.add(footerRole);
        navPanel.add(footerInfo);

        navPanel.revalidate(); navPanel.repaint();
    }

    private void addNavSection(String title) {
        JLabel sectionLabel = new JLabel("  " + title);
        sectionLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        sectionLabel.setForeground(TEXT_SECTION);
        sectionLabel.setBorder(new EmptyBorder(14, 16, 4, 16));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        navPanel.add(sectionLabel);
    }

    private void addNavButton(String text, String view) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        btn.setForeground(TEXT_DARK);
        btn.setBackground(BG_MAIN);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(9, 20, 9, 16),
                BorderFactory.createMatteBorder(0, 0, 0, 0, BG_MAIN)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(200, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setActionCommand(view);
        btn.addActionListener(e -> navigate(view));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(226, 232, 240)); }
            public void mouseExited(MouseEvent e) {
                if (!view.equals(currentView)) btn.setBackground(BG_MAIN);
            }
        });
        navButtons.add(btn);
        navPanel.add(btn);
    }

    private String currentView = VIEW_DASHBOARD;

    private void navigate(String view) {
        currentView = view;
        for (JButton btn : navButtons) {
            if (view.equals(btn.getActionCommand())) {
                btn.setBackground(new Color(219, 234, 254));
                btn.setForeground(PRIMARY);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(9, 20, 9, 16),
                        BorderFactory.createMatteBorder(0, 4, 0, 0, PRIMARY)));
            } else {
                btn.setBackground(BG_MAIN);
                btn.setForeground(TEXT_DARK);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(9, 20, 9, 16),
                        BorderFactory.createMatteBorder(0, 0, 0, 0, BG_MAIN)));
            }
        }

        // 使用集中管理的导航标题映射
        for (int i = 0; i < NAV_VIEWS.length; i++) {
            if (NAV_VIEWS[i].equals(view)) { topbarTitle.setText(NAV_TITLES[i]); break; }
        }

        switch (view) {
            case VIEW_DASHBOARD: refreshDashboard(); break;
            case VIEW_BOOKS: refreshBooks(); break;
            case VIEW_USERS: refreshUsers(); break;
            case VIEW_MY_BORROWS: refreshMyBorrows(); break;
            case VIEW_BORROW_MANAGE: refreshBorrowManage(); break;
            case VIEW_RESERVATIONS: refreshReservations(); break;
            case VIEW_NOTIFICATIONS: refreshNotifications(); break;
            case VIEW_SYSTEM: break;
        }
        contentCardLayout.show(contentPanel, view);
        statusLabel.setText("就绪");
    }

    // ==================== 仪表盘 ====================

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setName(VIEW_DASHBOARD);
        return panel;
    }

    private void refreshDashboard() {
        JPanel panel = dashboardPanel;
        panel.removeAll();
        try {
            List<Book> books = ctx.getBookService().getAllBooks();
            int totalBooks = books.size();
            int availCopies = books.stream().mapToInt(b -> b.getAvailableCopies() != null ? b.getAvailableCopies() : 0).sum();
            int totalCopies = books.stream().mapToInt(b -> b.getTotalCopies() != null ? b.getTotalCopies() : 0).sum();

            List<BorrowRecord> allRecords = ctx.getBorrowRecordDao().findAll();
            long activeBorrows = allRecords.stream().filter(r -> !r.isReturned()).count();
            long overdueCount = allRecords.stream().filter(r -> r.getStatus() == BorrowRecord.Status.BORROWED && r.isOverdue()).count();
            long activeReservations = ctx.getReservationDao().findAll().stream().filter(r -> r.getStatus() == Reservation.Status.ACTIVE).count();
            long unreadNotifs = ctx.getNotificationService().getUnreadNotifications(currentUser.getUserId()).size();

            JPanel statsGrid = new JPanel(new GridLayout(0, 4, 14, 14));
            statsGrid.setBackground(BG_MAIN);
            statsGrid.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
            statsGrid.add(createStatCard("藏", "图书总数", String.valueOf(totalBooks), "共 " + totalCopies + " 册", new Color(219, 234, 254), PRIMARY));
            statsGrid.add(createStatCard("可", "可借副本", String.valueOf(availCopies), "当前可借阅", new Color(209, 250, 229), SUCCESS));
            statsGrid.add(createStatCard("借", "当前借出", String.valueOf(activeBorrows), "逾期 " + overdueCount + " 本", new Color(254, 243, 199), WARNING));
            if (currentUser.isAdmin() || currentUser.isLibrarian()) {
                statsGrid.add(createStatCard("约", "活跃预约", String.valueOf(activeReservations), "等待到书", new Color(219, 234, 254), PRIMARY));
            } else {
                statsGrid.add(createStatCard("信", "未读消息", String.valueOf(unreadNotifs),
                        unreadNotifs > 0 ? "有新消息" : "无新消息",
                        unreadNotifs > 0 ? new Color(254, 226, 226) : new Color(209, 250, 229),
                        unreadNotifs > 0 ? DANGER : SUCCESS));
            }

            List<BorrowRecord> recentRecords = allRecords.stream()
                    .sorted((a, b) -> b.getBorrowDate().compareTo(a.getBorrowDate()))
                    .limit(8).collect(java.util.stream.Collectors.toList());
            String[] cols = {"图书", "借阅人", "借阅日期", "应还日期", "状态"};
            Object[][] data = new Object[recentRecords.size()][5];
            for (int i = 0; i < recentRecords.size(); i++) {
                BorrowRecord r = recentRecords.get(i);
                Book book = ctx.getBookDao().findById(r.getBookId());
                User user = ctx.getUserDao().findById(r.getUserId());
                data[i][0] = book != null ? book.getTitle() : "未知";
                data[i][1] = user != null ? user.getName() : "未知";
                data[i][2] = r.getBorrowDate().format(FMT);
                data[i][3] = r.getDueDate().format(FMT);
                data[i][4] = r.isReturned() ? "已归还" : (r.isOverdue() ? "已逾期" : "借阅中");
            }
            JPanel recentPanel = createTablePanel("最近借阅记录", cols, data);
            panel.add(statsGrid);
            panel.add(Box.createVerticalStrut(18));
            panel.add(recentPanel);
        } catch (Exception e) {
            panel.add(new JLabel("加载失败: " + e.getMessage()));
        }
        panel.revalidate(); panel.repaint();
    }

    private JPanel createStatCard(String icon, String label, String value, String sub, Color bg, Color fg) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_LIGHT, 1, true), new EmptyBorder(16, 16, 16, 16)));
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        iconLabel.setForeground(fg);
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setBackground(bg);
        iconPanel.setPreferredSize(new Dimension(48, 48));
        iconPanel.add(iconLabel);
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        valueLabel.setForeground(TEXT_DARK);
        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(FONT_SMALL);
        labelLbl.setForeground(TEXT_GRAY);
        textPanel.add(valueLabel); textPanel.add(labelLbl);
        card.add(iconPanel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    // ==================== 图书列表 ====================

    private JPanel createBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_BOOKS);
        return panel;
    }

    private void refreshBooks() { refreshBooks(""); }

    private void refreshBooks(String keyword) {
        JPanel panel = booksPanel;
        panel.removeAll();

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(BG_MAIN);
        topPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 42));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.setBackground(BG_MAIN);
        JTextField searchField = new JTextField(16);
        searchField.setFont(FONT_NORMAL);
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.setText(keyword);
        JButton searchBtn = new JButton("搜索");
        styleButton(searchBtn, PRIMARY, false);
        searchBtn.addActionListener(e -> refreshBooks(searchField.getText().trim()));
        JButton clearBtn = new JButton("清空");
        styleButton(clearBtn, GRAY_BTN, false);
        clearBtn.addActionListener(e -> refreshBooks(""));
        searchPanel.add(searchField); searchPanel.add(searchBtn); searchPanel.add(clearBtn);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setBackground(BG_MAIN);
        if (currentUser.isAdmin() || currentUser.isLibrarian()) {
            JButton addBtn = new JButton("新书上架");
            styleButton(addBtn, PRIMARY, false);
            addBtn.addActionListener(e -> showBookDialog(null));
            btnPanel.add(addBtn);
        }
        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(btnPanel, BorderLayout.EAST);

        List<Book> books = keyword.isEmpty() ? ctx.getBookService().getAllBooks()
                : ctx.getBookService().searchBooks(keyword);

        String[] cols = {"ISBN", "书名", "作者", "分类", "位置", "馆藏/可借", "操作"};
        Object[][] data = new Object[books.size()][7];
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            int j = 0;
            data[i][j++] = b.getIsbn();
            data[i][j++] = b.getTitle();
            data[i][j++] = b.getAuthor();
            data[i][j++] = b.getCategory();
            data[i][j++] = b.getLocation();
            data[i][j++] = (b.getTotalCopies() != null ? b.getTotalCopies() : 0) + " / " +
                    (b.getAvailableCopies() != null ? b.getAvailableCopies() : 0);
            data[i][j++] = b.getBookId();
        }

        JTable table = createStyledTable(cols, data);

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int row, int col) {
                String s = String.valueOf(v);
                setText(s); setFont(FONT_NORMAL);
                if (s.contains("/")) {
                    try {
                        int avail = Integer.parseInt(s.split("/")[1].trim());
                        setForeground(avail > 0 ? SUCCESS : DANGER);
                    } catch (NumberFormatException ex) { setForeground(TEXT_DARK); }
                }
                setBackground(sel ? new Color(219, 234, 254) : Color.WHITE);
                return this;
            }
        });

        if (currentUser.isAdmin() || currentUser.isLibrarian() || currentUser.isBorrower()) {
            table.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
            table.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(table, books));
            table.getColumnModel().getColumn(6).setPreferredWidth(180);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
        scrollPane.setBackground(Color.WHITE);

        JLabel countLabel = new JLabel("共 " + books.size() + " 本图书");
        countLabel.setFont(FONT_SMALL);
        countLabel.setForeground(TEXT_GRAY);
        countLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(countLabel, BorderLayout.SOUTH);
        panel.revalidate(); panel.repaint();
    }

    // ==================== 用户管理 ====================

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_USERS);
        return panel;
    }

    private void refreshUsers() {
        JPanel panel = usersPanel;
        panel.removeAll();

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(BG_MAIN);
        JButton addBtn = new JButton("新建用户");
        styleButton(addBtn, PRIMARY, false);
        addBtn.addActionListener(e -> showUserDialog(null));
        topPanel.add(addBtn);

        List<User> users = ctx.getUserService().getAllUsers(currentUser);
        String[] cols = {"ID", "用户名", "姓名", "角色", "状态", "罚金", "操作"};
        Object[][] data = new Object[users.size()][7];
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            data[i][0] = u.getUserId();
            data[i][1] = u.getUsername();
            data[i][2] = u.getName();
            data[i][3] = u.getRole().getDescription();
            data[i][4] = u.getStatus().name();
            data[i][5] = "Y" + String.format("%.2f", u.getUnpaidFine());
            data[i][6] = u.getUserId();
        }

        JTable table = createStyledTable(cols, data);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int row, int col) {
                setText(String.valueOf(v)); setFont(FONT_SMALL);
                setHorizontalAlignment(SwingConstants.CENTER);
                if ("ACTIVE".equals(v)) { setForeground(new Color(6, 95, 70)); setBackground(sel ? new Color(219, 234, 254) : new Color(209, 250, 229)); }
                else { setForeground(new Color(100, 116, 139)); setBackground(sel ? new Color(219, 234, 254) : new Color(241, 245, 249)); }
                return this;
            }
        });
        table.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new UserButtonEditor(table, users));
        table.getColumnModel().getColumn(6).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.revalidate(); panel.repaint();
    }

    // ==================== 借阅管理(管理员) ====================

    private JPanel createBorrowManagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_BORROW_MANAGE);
        return panel;
    }

    private void refreshBorrowManage() {
        JPanel panel = borrowManagePanel;
        panel.removeAll();

        List<BorrowRecord> allRecords = ctx.getBorrowRecordDao().findAll();
        List<BorrowRecord> active = allRecords.stream()
                .filter(r -> !r.isReturned())
                .sorted((a, b) -> b.getBorrowDate().compareTo(a.getBorrowDate()))
                .collect(java.util.stream.Collectors.toList());
        List<BorrowRecord> overdue = active.stream()
                .filter(r -> r.getStatus() == BorrowRecord.Status.BORROWED && r.isOverdue())
                .collect(java.util.stream.Collectors.toList());

        String[] cols = {"记录ID", "图书", "借阅人", "借阅日期", "应还日期", "状态", "续借次数", "操作"};
        Object[][] data = new Object[active.size()][8];
        for (int i = 0; i < active.size(); i++) {
            BorrowRecord r = active.get(i);
            Book book = ctx.getBookDao().findById(r.getBookId());
            User user = ctx.getUserDao().findById(r.getUserId());
            data[i][0] = r.getRecordId();
            data[i][1] = book != null ? book.getTitle() : "未知";
            data[i][2] = user != null ? user.getName() : "未知";
            data[i][3] = r.getBorrowDate().format(FMT);
            data[i][4] = r.getDueDate().format(FMT);
            data[i][5] = r.isOverdue() ? "已逾期" : "正常";
            data[i][6] = r.getRenewCount() + "/2";
            data[i][7] = r.getRecordId();
        }

        JPanel activePanel = createCardPanel("当前借出 (" + active.size() + ")");
        if (active.isEmpty()) {
            activePanel.add(createEmptyLabel("暂无借出记录"));
        } else {
            JTable table = createStyledTable(cols, data);
            hideColumn(table, 0); hideColumn(table, 7);
            table.getColumnModel().getColumn(5).setCellRenderer(createStatusRenderer());

            JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actionBar.setBackground(Color.WHITE);
            JButton returnBtn = new JButton("归还选中");
            styleButton(returnBtn, SUCCESS, false);
            returnBtn.addActionListener(ev -> {
                int selRow = table.getSelectedRow();
                if (selRow < 0) { showError("请先选中一条记录"); return; }
                int modelRow = table.convertRowIndexToModel(selRow);
                Object idObj = table.getModel().getValueAt(modelRow, 7);
                Long recordId = idObj instanceof Long ? (Long) idObj : null;
                if (recordId != null) {
                    int opt = JOptionPane.showConfirmDialog(frame, "确认归还该图书？", "归还图书", JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) {
                        try {
                            ctx.getBorrowService().returnBookByRecordId(recordId, currentUser);
                            showToast("归还成功"); refreshBorrowManage();
                        } catch (Exception ex) { showError(ex.getMessage()); }
                    }
                }
            });
            actionBar.add(returnBtn);
            activePanel.add(actionBar, BorderLayout.SOUTH);
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            activePanel.add(sp, BorderLayout.CENTER);
        }

        if (!overdue.isEmpty()) {
            JPanel overduePanel = createCardPanel("逾期未还 (" + overdue.size() + ")");
            overduePanel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(DANGER, 1, true), new EmptyBorder(12, 12, 12, 12)));
            String[] overCols = {"图书", "借阅人", "应还日期", "罚金"};
            Object[][] overData = new Object[overdue.size()][4];
            for (int i = 0; i < overdue.size(); i++) {
                BorrowRecord r = overdue.get(i);
                Book book = ctx.getBookDao().findById(r.getBookId());
                User user = ctx.getUserDao().findById(r.getUserId());
                overData[i][0] = book != null ? book.getTitle() : "未知";
                overData[i][1] = user != null ? user.getName() : "未知";
                overData[i][2] = r.getDueDate().format(FMT);
                overData[i][3] = "Y" + String.format("%.2f", r.getFineAmount());
            }
            JTable overTable = createStyledTable(overCols, overData);
            overTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                               boolean focus, int row, int col) {
                    setText(String.valueOf(v)); setFont(FONT_NORMAL);
                    setForeground(DANGER);
                    setBackground(sel ? new Color(219, 234, 254) : Color.WHITE);
                    return this;
                }
            });
            JScrollPane overSp = new JScrollPane(overTable);
            overSp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            overduePanel.add(overSp);
            panel.add(overduePanel);
            panel.add(Box.createVerticalStrut(12));
        }
        panel.add(activePanel);
        panel.revalidate(); panel.repaint();
    }

    // ==================== 我的借阅 ====================

    private JPanel createMyBorrowsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_MY_BORROWS);
        return panel;
    }

    private void refreshMyBorrows() {
        JPanel panel = myBorrowsPanel;
        panel.removeAll();

        User me = ctx.getUserService().getUserById(currentUser.getUserId());
        if (me != null && me.getUnpaidFine() > 0) {
            JPanel fineAlert = new JPanel(new BorderLayout());
            fineAlert.setBackground(new Color(254, 242, 242));
            fineAlert.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(DANGER, 1, true), new EmptyBorder(12, 16, 12, 16)));
            fineAlert.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            JLabel fineLabel = new JLabel("待缴罚金：Y" + String.format("%.2f", me.getUnpaidFine()));
            fineLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            fineLabel.setForeground(DANGER);
            JButton payBtn = new JButton("缴纳罚金");
            styleButton(payBtn, DANGER, false);
            payBtn.addActionListener(e -> showPayFineDialog(me));
            fineAlert.add(fineLabel, BorderLayout.WEST);
            fineAlert.add(payBtn, BorderLayout.EAST);
            panel.add(fineAlert);
            panel.add(Box.createVerticalStrut(12));
        }

        List<BorrowRecord> allRecords = ctx.getBorrowService().getUserRecords(currentUser.getUserId());
        List<BorrowRecord> unreturned = allRecords.stream().filter(r -> !r.isReturned()).collect(java.util.stream.Collectors.toList());
        List<BorrowRecord> returned = allRecords.stream().filter(r -> r.isReturned()).collect(java.util.stream.Collectors.toList());

        JPanel unreturnedPanel = createCardPanel("当前借阅 (" + unreturned.size() + ")");
        if (unreturned.isEmpty()) {
            unreturnedPanel.add(createEmptyLabel("暂无借阅"));
        } else {
            String[] cols = {"记录ID", "图书", "借阅日期", "应还日期", "状态", "罚金", "操作"};
            Object[][] data = new Object[unreturned.size()][7];
            for (int i = 0; i < unreturned.size(); i++) {
                BorrowRecord r = unreturned.get(i);
                Book book = ctx.getBookDao().findById(r.getBookId());
                data[i][0] = r.getRecordId();
                data[i][1] = book != null ? book.getTitle() : "未知";
                data[i][2] = r.getBorrowDate().format(FMT);
                data[i][3] = r.getDueDate().format(FMT);
                data[i][4] = r.isOverdue() ? "已逾期" : "正常";
                data[i][5] = "Y" + String.format("%.2f", r.getFineAmount());
                data[i][6] = r.getRecordId();
            }
            JTable table = createStyledTable(cols, data);
            hideColumn(table, 0); hideColumn(table, 6);
            table.getColumnModel().getColumn(4).setCellRenderer(createStatusRenderer());

            JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actionBar.setBackground(Color.WHITE);
            JButton returnBtn = new JButton("归还选中");
            styleButton(returnBtn, SUCCESS, false);
            returnBtn.addActionListener(ev -> {
                int selRow = table.getSelectedRow();
                if (selRow < 0) { showError("请先选中一条记录"); return; }
                int modelRow = table.convertRowIndexToModel(selRow);
                Object idObj = table.getModel().getValueAt(modelRow, 6);
                Long recordId = idObj instanceof Long ? (Long) idObj : null;
                if (recordId != null) {
                    int opt = JOptionPane.showConfirmDialog(frame, "确认归还该图书？如有逾期将自动计算罚金。", "归还图书", JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) {
                        try {
                            ctx.getBorrowService().returnBookByRecordId(recordId, currentUser);
                            showToast("归还成功"); refreshMyBorrows();
                        } catch (Exception ex) { showError(ex.getMessage()); }
                    }
                }
            });
            actionBar.add(returnBtn);

            JButton renewBtn = new JButton("续借选中");
            styleButton(renewBtn, GRAY_BTN, false);
            renewBtn.addActionListener(ev -> {
                int selRow = table.getSelectedRow();
                if (selRow < 0) { showError("请先选中一条记录"); return; }
                int modelRow = table.convertRowIndexToModel(selRow);
                Object idObj = table.getModel().getValueAt(modelRow, 6);
                Long recordId = idObj instanceof Long ? (Long) idObj : null;
                if (recordId != null) {
                    BorrowRecord rec = unreturned.stream().filter(r -> r.getRecordId().equals(recordId)).findFirst().orElse(null);
                    if (rec != null && rec.getRenewCount() >= 2) { showError("已达到最大续借次数"); return; }
                    if (rec != null && rec.isOverdue()) { showError("已逾期图书无法续借"); return; }
                    int opt = JOptionPane.showConfirmDialog(frame, "确认续借？可延长15天。", "续借图书", JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) {
                        try {
                            BorrowRecord r = ctx.getBorrowService().renewBook(recordId, currentUser);
                            showToast("续借成功！新应还日期: " + r.getDueDate().format(FMT));
                            refreshMyBorrows();
                        } catch (Exception ex) { showError(ex.getMessage()); }
                    }
                }
            });
            actionBar.add(renewBtn);
            unreturnedPanel.add(actionBar, BorderLayout.SOUTH);
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            unreturnedPanel.add(sp, BorderLayout.CENTER);
        }

        JPanel returnedPanel = createCardPanel("历史记录 (" + returned.size() + ")");
        if (returned.isEmpty()) {
            returnedPanel.add(createEmptyLabel("暂无历史记录"));
        } else {
            String[] cols2 = {"图书", "借阅日期", "归还日期", "罚金"};
            Object[][] data2 = new Object[returned.size()][4];
            for (int i = 0; i < returned.size(); i++) {
                BorrowRecord r = returned.get(i);
                Book book = ctx.getBookDao().findById(r.getBookId());
                data2[i][0] = book != null ? book.getTitle() : "未知";
                data2[i][1] = r.getBorrowDate().format(FMT);
                data2[i][2] = r.getReturnDate() != null ? r.getReturnDate().format(FMT) : "-";
                data2[i][3] = "Y" + String.format("%.2f", r.getFineAmount());
            }
            JTable table2 = createStyledTable(cols2, data2);
            JScrollPane sp2 = new JScrollPane(table2);
            sp2.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            returnedPanel.add(sp2);
        }

        panel.add(unreturnedPanel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(returnedPanel);
        panel.revalidate(); panel.repaint();
    }

    // ==================== 预约管理 ====================

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_RESERVATIONS);
        return panel;
    }

    private void refreshReservations() {
        JPanel panel = reservationsPanel;
        panel.removeAll();

        List<Reservation> myReservations;
        if (currentUser.isAdmin() || currentUser.isLibrarian()) {
            myReservations = ctx.getReservationDao().findAll();
        } else {
            myReservations = ctx.getReservationDao().findAll().stream()
                    .filter(r -> r.getUserId().equals(currentUser.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        List<Reservation> active = myReservations.stream()
                .filter(r -> r.getStatus() == Reservation.Status.ACTIVE)
                .collect(java.util.stream.Collectors.toList());
        List<Reservation> others = myReservations.stream()
                .filter(r -> r.getStatus() != Reservation.Status.ACTIVE)
                .collect(java.util.stream.Collectors.toList());

        JPanel activePanel = createCardPanel("活跃预约 (" + active.size() + ")");
        if (active.isEmpty()) {
            activePanel.add(createEmptyLabel("暂无活跃预约"));
        } else {
            boolean showUser = currentUser.isAdmin() || currentUser.isLibrarian();
            String[] cols = showUser ?
                    new String[]{"ID", "图书", "作者", "预约人", "预约日期", "状态", "操作"} :
                    new String[]{"ID", "图书", "作者", "预约日期", "状态", "操作"};
            Object[][] data = new Object[active.size()][cols.length];
            for (int i = 0; i < active.size(); i++) {
                Reservation r = active.get(i);
                Book book = ctx.getBookDao().findById(r.getBookId());
                User user = ctx.getUserDao().findById(r.getUserId());
                int j = 0;
                data[i][j++] = r.getReservationId();
                data[i][j++] = book != null ? book.getTitle() : "未知";
                data[i][j++] = book != null ? book.getAuthor() : "未知";
                if (showUser) data[i][j++] = user != null ? user.getName() : "未知";
                data[i][j++] = r.getRequestDate().format(FMT);
                data[i][j++] = r.getStatus().getDescription();
                data[i][j++] = r.getReservationId();
            }
            JTable table = createStyledTable(cols, data);
            hideColumn(table, 0);
            table.getColumnModel().getColumn(cols.length - 1).setCellRenderer(new ButtonRenderer());
            table.getColumnModel().getColumn(cols.length - 1).setCellEditor(new ResCancelEditor(table, active));
            table.getColumnModel().getColumn(cols.length - 1).setPreferredWidth(100);
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            activePanel.add(sp);
        }
        panel.add(activePanel);

        if (!others.isEmpty()) {
            panel.add(Box.createVerticalStrut(12));
            JPanel othersPanel = createCardPanel("历史预约 (" + others.size() + ")");
            String[] cols2 = {"图书", "预约日期", "状态"};
            Object[][] data2 = new Object[others.size()][3];
            for (int i = 0; i < others.size(); i++) {
                Reservation r = others.get(i);
                Book book = ctx.getBookDao().findById(r.getBookId());
                data2[i][0] = book != null ? book.getTitle() : "未知";
                data2[i][1] = r.getRequestDate().format(FMT);
                data2[i][2] = r.getStatus().getDescription();
            }
            JTable table2 = createStyledTable(cols2, data2);
            JScrollPane sp2 = new JScrollPane(table2);
            sp2.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            othersPanel.add(sp2);
            panel.add(othersPanel);
        }
        panel.revalidate(); panel.repaint();
    }

    // ==================== 消息通知 ====================

    private JPanel createNotificationsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_NOTIFICATIONS);
        return panel;
    }

    private void refreshNotifications() {
        JPanel panel = notificationsPanel;
        panel.removeAll();

        List<Notification> notifs = ctx.getNotificationService().getAllNotifications(currentUser.getUserId());
        notifs.sort((a, b) -> {
            if (a.getSendTime() == null) return 1;
            if (b.getSendTime() == null) return -1;
            return b.getSendTime().compareTo(a.getSendTime());
        });

        java.util.Map<String, String> typeLabels = new java.util.HashMap<>();
        typeLabels.put("FINE", "[罚金]");
        typeLabels.put("OVERDUE_REMINDER", "[逾期]");
        typeLabels.put("RESERVATION_SUCCESS", "[预约]");
        typeLabels.put("BOOK_AVAILABLE", "[到书]");
        typeLabels.put("OUT_OF_STOCK", "[缺货]");
        typeLabels.put("SYSTEM", "[系统]");

        JPanel cardPanel = createCardPanel("消息列表 (" + notifs.size() + ")");
        if (notifs.isEmpty()) {
            cardPanel.add(createEmptyLabel("暂无消息"));
        } else {
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setBackground(Color.WHITE);

            for (Notification n : notifs) {
                JPanel item = new JPanel(new BorderLayout(10, 0));
                item.setBackground(n.isRead() ? Color.WHITE : new Color(219, 234, 254));
                item.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(241, 245, 249), 0, false),
                        new EmptyBorder(10, 12, 10, 12)));
                item.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

                String label = typeLabels.getOrDefault(n.getType() != null ? n.getType().name() : "SYSTEM", "[通知]");
                JLabel iconLabel = new JLabel(label);
                iconLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));

                JPanel textPanel = new JPanel();
                textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                textPanel.setBackground(item.getBackground());
                JLabel contentLabel = new JLabel("<html><div style='max-width:480px'>" +
                        escapeHtml(n.getContent()) + "</div></html>");
                contentLabel.setFont(FONT_NORMAL);
                JLabel timeLabel = new JLabel(n.getSendTime() != null ? n.getSendTime().format(FMT) : "");
                timeLabel.setFont(FONT_SMALL);
                timeLabel.setForeground(TEXT_GRAY);
                textPanel.add(contentLabel); textPanel.add(timeLabel);

                JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                rightPanel.setBackground(item.getBackground());
                if (!n.isRead()) {
                    JButton readBtn = new JButton("标记已读");
                    styleButton(readBtn, PRIMARY, false);
                    readBtn.addActionListener(e -> {
                        ctx.getNotificationService().markAsRead(n.getNotificationId());
                        refreshNotifications();
                    });
                    rightPanel.add(readBtn);
                } else {
                    JLabel readLabel = new JLabel("已读");
                    readLabel.setFont(FONT_SMALL);
                    readLabel.setForeground(TEXT_GRAY);
                    rightPanel.add(readLabel);
                }

                item.add(iconLabel, BorderLayout.WEST);
                item.add(textPanel, BorderLayout.CENTER);
                item.add(rightPanel, BorderLayout.EAST);
                listPanel.add(item);
                listPanel.add(Box.createVerticalStrut(2));
            }
            JScrollPane sp = new JScrollPane(listPanel);
            sp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            sp.setPreferredSize(new Dimension(780, 400));
            cardPanel.add(sp);
        }
        panel.add(cardPanel);
        panel.revalidate(); panel.repaint();
    }

    // ==================== 系统操作 ====================

    private JPanel createSystemPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setName(VIEW_SYSTEM);

        JPanel cardPanel = createCardPanel("系统操作");
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        JButton overdueBtn = new JButton("发送逾期提醒");
        styleButton(overdueBtn, WARNING, false);
        overdueBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        overdueBtn.setMaximumSize(new Dimension(300, 40));
        overdueBtn.addActionListener(e -> {
            int count = ctx.getBorrowService().sendOverdueReminders();
            showToast("已发送 " + count + " 条逾期提醒");
        });

        JButton oosBtn = new JButton("发送缺货通知");
        styleButton(oosBtn, GRAY_BTN, false);
        oosBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        oosBtn.setMaximumSize(new Dimension(300, 40));
        oosBtn.addActionListener(e -> {
            int count = ctx.getReservationService().sendOutOfStockNotifications();
            showToast("已发送 " + count + " 条缺货通知");
        });

        cardPanel.add(overdueBtn);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(oosBtn);
        panel.add(cardPanel);
        return panel;
    }

    // ==================== 对话框 ====================

    private void showBookDialog(Long bookId) {
        JDialog dialog = new JDialog(frame, bookId == null ? "新书上架" : "编辑图书", true);
        dialog.setSize(500, bookId == null ? 420 : 380);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        Book book = new Book();
        if (bookId != null) {
            Book existing = ctx.getBookService().getBookById(bookId);
            if (existing != null) book = existing;
        }

        JTextField isbnField = new JTextField(book.getIsbn() != null ? book.getIsbn() : "");
        JTextField titleField = new JTextField(book.getTitle() != null ? book.getTitle() : "");
        JTextField authorField = new JTextField(book.getAuthor() != null ? book.getAuthor() : "");
        JTextField publisherField = new JTextField(book.getPublisher() != null ? book.getPublisher() : "");
        JTextField categoryField = new JTextField(book.getCategory() != null ? book.getCategory() : "");
        JTextField locationField = new JTextField(book.getLocation() != null ? book.getLocation() : "");
        JTextField dateField = new JTextField(book.getPublishDate() != null ? book.getPublishDate().format(FMT) : "");
        JTextField qtyField = new JTextField("1");

        if (bookId != null) isbnField.setEditable(false);

        form.add(createFormRow("ISBN *:", isbnField));
        form.add(createFormRow("书名 *:", titleField));
        form.add(createFormRow("作者:", authorField));
        form.add(createFormRow("出版社:", publisherField));
        form.add(createFormRow("分类:", categoryField));
        form.add(createFormRow("馆藏位置:", locationField));
        form.add(createFormRow("出版日期(yyyy-MM-dd):", dateField));
        if (bookId == null) form.add(createFormRow("入库数量:", qtyField));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton saveBtn = new JButton(bookId == null ? "上架" : "保存");
        styleButton(saveBtn, PRIMARY, false);

        saveBtn.addActionListener(e -> {
            try {
                String isbn = isbnField.getText().trim();
                String title = titleField.getText().trim();
                if (isbn.isEmpty() || title.isEmpty()) { showError("ISBN和书名不能为空"); return; }
                if (bookId == null) {
                    Book newBook = new Book();
                    newBook.setIsbn(isbn); newBook.setTitle(title);
                    newBook.setAuthor(getOpt(authorField)); newBook.setPublisher(getOpt(publisherField));
                    newBook.setCategory(getOpt(categoryField)); newBook.setLocation(getOpt(locationField));
                    String dateStr = dateField.getText().trim();
                    if (!dateStr.isEmpty()) newBook.setPublishDate(LocalDate.parse(dateStr, FMT));
                    int qty = Integer.parseInt(qtyField.getText().trim());
                    ctx.getBookService().addBook(newBook, qty, currentUser);
                    showToast("新书上架成功！");
                } else {
                    Book update = new Book();
                    update.setTitle(title); update.setAuthor(getOpt(authorField));
                    update.setPublisher(getOpt(publisherField)); update.setCategory(getOpt(categoryField));
                    update.setLocation(getOpt(locationField));
                    String dateStr = dateField.getText().trim();
                    if (!dateStr.isEmpty()) update.setPublishDate(LocalDate.parse(dateStr, FMT));
                    ctx.getBookService().updateBookInfo(bookId, update, currentUser);
                    showToast("图书信息已更新！");
                }
                dialog.dispose(); refreshBooks();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });

        btnPanel.add(cancelBtn); btnPanel.add(saveBtn);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showUserDialog(Long userId) {
        JDialog dialog = new JDialog(frame, userId == null ? "新建用户" : "编辑用户", true);
        dialog.setSize(450, userId == null ? 380 : 340);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        User user = new User();
        user.setRole(User.Role.BORROWER);
        user.setStatus(User.Status.ACTIVE);
        if (userId != null) {
            User existing = ctx.getUserService().getUserById(userId);
            if (existing != null) user = existing;
        }

        JTextField unameField = new JTextField(user.getUsername() != null ? user.getUsername() : "");
        JTextField nameField = new JTextField(user.getName() != null ? user.getName() : "");
        JTextField emailField = new JTextField(user.getEmail() != null ? user.getEmail() : "");
        JTextField phoneField = new JTextField(user.getPhone() != null ? user.getPhone() : "");
        JPasswordField pwdField = new JPasswordField();

        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"借阅者", "图书管理员", "系统管理员"});
        if (user.getRole() == User.Role.LIBRARIAN) roleCombo.setSelectedIndex(1);
        else if (user.getRole() == User.Role.ADMIN) roleCombo.setSelectedIndex(2);
        else roleCombo.setSelectedIndex(0);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"正常", "禁用"});
        if (user.getStatus() == User.Status.INACTIVE) statusCombo.setSelectedIndex(1);

        if (userId == null) {
            form.add(createFormRow("用户名 *:", unameField));
            form.add(createFormRow("密码 *:", pwdField));
        }
        form.add(createFormRow("姓名:", nameField));
        form.add(createFormRow("邮箱:", emailField));
        form.add(createFormRow("电话:", phoneField));
        form.add(createFormRow("角色:", roleCombo));
        if (userId != null) form.add(createFormRow("状态:", statusCombo));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton saveBtn = new JButton(userId == null ? "创建" : "保存");
        styleButton(saveBtn, PRIMARY, false);

        saveBtn.addActionListener(e -> {
            try {
                if (userId == null) {
                    String uname = unameField.getText().trim();
                    String pwd = new String(pwdField.getPassword());
                    if (uname.isEmpty() || pwd.isEmpty()) { showError("用户名和密码不能为空"); return; }
                    if (pwd.length() < 6) { showError("密码至少需要6位"); return; }
                    User.Role role = User.Role.BORROWER;
                    switch (roleCombo.getSelectedIndex()) {
                        case 1: role = User.Role.LIBRARIAN; break;
                        case 2: role = User.Role.ADMIN; break;
                    }
                    ctx.getUserService().createUser(uname, pwd, getOpt(nameField), role, User.Status.ACTIVE,
                            getOpt(emailField), getOpt(phoneField), currentUser);
                    showToast("用户创建成功！");
                } else {
                    User update = new User();
                    update.setUsername(getOpt(unameField)); update.setName(getOpt(nameField));
                    update.setEmail(getOpt(emailField)); update.setPhone(getOpt(phoneField));
                    switch (roleCombo.getSelectedIndex()) {
                        case 0: update.setRole(User.Role.BORROWER); break;
                        case 1: update.setRole(User.Role.LIBRARIAN); break;
                        case 2: update.setRole(User.Role.ADMIN); break;
                    }
                    update.setStatus(statusCombo.getSelectedIndex() == 0 ? User.Status.ACTIVE : User.Status.INACTIVE);
                    String pwd = new String(pwdField.getPassword());
                    if (!pwd.isEmpty()) {
                        if (pwd.length() < 6) { showError("密码至少需要6位"); return; }
                        update.setPassword(pwd);
                    }
                    ctx.getUserService().updateUser(userId, update, currentUser);
                    showToast("用户信息已更新！");
                }
                dialog.dispose(); refreshUsers();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });

        btnPanel.add(cancelBtn); btnPanel.add(saveBtn);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showPayFineDialog(User user) {
        JDialog dialog = new JDialog(frame, "缴纳罚金", true);
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel infoLabel = new JLabel("当前欠款: Y" + String.format("%.2f", user.getUnpaidFine()));
        infoLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(infoLabel);
        form.add(Box.createVerticalStrut(10));

        JTextField amountField = new JTextField(String.format("%.2f", user.getUnpaidFine()));
        form.add(createFormRow("缴纳金额:", amountField));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton payBtn = new JButton("确认缴纳");
        styleButton(payBtn, DANGER, false);
        payBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                User updated = ctx.getUserService().payFine(currentUser.getUserId(), amount);
                currentUser = updated;
                showToast("缴纳成功！剩余: Y" + String.format("%.2f", updated.getUnpaidFine()));
                dialog.dispose(); refreshMyBorrows();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });

        btnPanel.add(cancelBtn); btnPanel.add(payBtn);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showRemoveBookDialog(Long bookId, String title) {
        JDialog dialog = new JDialog(frame, "下架图书", true);
        dialog.setSize(380, 220);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel info = new JLabel("确认下架《" + title + "》？");
        info.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        form.add(info);
        form.add(Box.createVerticalStrut(10));

        JTextField qtyField = new JTextField("1");
        JTextField reasonField = new JTextField();
        form.add(createFormRow("下架数量:", qtyField));
        form.add(createFormRow("原因:", reasonField));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton confirmBtn = new JButton("确认下架");
        styleButton(confirmBtn, DANGER, false);
        confirmBtn.addActionListener(e -> {
            try {
                int qty = Integer.parseInt(qtyField.getText().trim());
                ctx.getBookService().removeBook(bookId, qty, reasonField.getText().trim(), currentUser);
                showToast("图书已下架"); dialog.dispose(); refreshBooks();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });

        btnPanel.add(cancelBtn); btnPanel.add(confirmBtn);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ==================== 表格编辑器（内部类） ====================

    class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel;
        private final transient List<Book> books;
        private Long bookId;

        public ButtonEditor(JTable table, List<Book> books) {
            this.books = books;
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            panel.setOpaque(false);
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.removeAll();
            int modelRow = table.convertRowIndexToModel(row);
            Object idObj = table.getModel().getValueAt(modelRow, table.getColumnCount() - 1);
            bookId = idObj instanceof Long ? (Long) idObj : null;
            Book book = findBook(bookId);
            boolean canEdit = currentUser.isAdmin() || currentUser.isLibrarian();
            boolean isBorrower = currentUser.isBorrower();

            if (canEdit) {
                JButton editBtn = new JButton("编辑");
                styleButton(editBtn, GRAY_BTN, false);
                editBtn.addActionListener(ev -> { fireEditingStopped(); showBookDialog(bookId); });
                panel.add(editBtn);
                JButton removeBtn = new JButton("下架");
                styleButton(removeBtn, DANGER, false);
                removeBtn.addActionListener(ev -> {
                    fireEditingStopped();
                    showRemoveBookDialog(bookId, book != null ? book.getTitle() : "");
                });
                panel.add(removeBtn);
            }
            if (isBorrower && book != null) {
                if (book.getAvailableCopies() != null && book.getAvailableCopies() > 0) {
                    JButton borrowBtn = new JButton("借阅");
                    styleButton(borrowBtn, PRIMARY, false);
                    borrowBtn.addActionListener(ev -> {
                        fireEditingStopped();
                        try {
                            ctx.getBorrowService().borrowBook(currentUser.getUserId(), bookId, currentUser);
                            showToast("借阅成功！"); refreshBooks();
                        } catch (Exception ex) { showError(ex.getMessage()); }
                    });
                    panel.add(borrowBtn);
                } else {
                    JButton reserveBtn = new JButton("预约");
                    styleButton(reserveBtn, WARNING, false);
                    reserveBtn.addActionListener(ev -> {
                        fireEditingStopped();
                        try {
                            ctx.getReservationService().reserveBook(currentUser.getUserId(), bookId);
                            showToast("预约成功！");
                        } catch (Exception ex) { showError(ex.getMessage()); }
                    });
                    panel.add(reserveBtn);
                }
            }
            return panel;
        }
        public Object getCellEditorValue() { return bookId; }
    }

    class UserButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel;
        private final transient List<User> users;
        private Long userId;

        public UserButtonEditor(JTable table, List<User> users) {
            this.users = users;
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            panel.setOpaque(false);
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.removeAll();
            int modelRow = table.convertRowIndexToModel(row);
            Object idObj = table.getModel().getValueAt(modelRow, 6);
            userId = idObj instanceof Long ? (Long) idObj : null;
            User user = findUser(userId);
            if (user == null) return panel;

            JButton editBtn = new JButton("编辑");
            styleButton(editBtn, GRAY_BTN, false);
            editBtn.addActionListener(ev -> { fireEditingStopped(); showUserDialog(userId); });
            panel.add(editBtn);

            if (user.getStatus() == User.Status.ACTIVE) {
                JButton disableBtn = new JButton("禁用");
                styleButton(disableBtn, WARNING, false);
                disableBtn.addActionListener(ev -> {
                    fireEditingStopped();
                    int opt = JOptionPane.showConfirmDialog(frame, "确认禁用该用户？", "禁用用户", JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) {
                        try { ctx.getUserService().disableUser(userId, currentUser); showToast("用户已禁用"); refreshUsers(); }
                        catch (Exception ex) { showError(ex.getMessage()); }
                    }
                });
                panel.add(disableBtn);
            } else {
                JButton enableBtn = new JButton("启用");
                styleButton(enableBtn, SUCCESS, false);
                enableBtn.addActionListener(ev -> {
                    fireEditingStopped();
                    try { ctx.getUserService().enableUser(userId, currentUser); showToast("用户已启用"); refreshUsers(); }
                    catch (Exception ex) { showError(ex.getMessage()); }
                });
                panel.add(enableBtn);
            }

            JButton deleteBtn = new JButton("删除");
            styleButton(deleteBtn, DANGER, false);
            deleteBtn.addActionListener(ev -> {
                fireEditingStopped();
                int opt = JOptionPane.showConfirmDialog(frame,
                        "确认删除用户「" + user.getUsername() + "」？此操作不可恢复。",
                        "删除用户", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    try {
                        if (user.getUnpaidFine() > 0) { showError("该用户尚有未缴纳罚金，需先缴清"); return; }
                        List<BorrowRecord> unreturned = ctx.getBorrowRecordDao().findUnreturnedByUserId(userId);
                        if (!unreturned.isEmpty()) { showError("该用户尚有 " + unreturned.size() + " 本图书未归还，无法删除"); return; }
                        ctx.getUserService().deleteUser(userId, currentUser);
                        showToast("用户已删除"); refreshUsers();
                    } catch (Exception ex) { showError(ex.getMessage()); }
                }
            });
            panel.add(deleteBtn);
            return panel;
        }
        public Object getCellEditorValue() { return userId; }
    }

    class ResCancelEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel;
        private final transient List<Reservation> reservations;

        public ResCancelEditor(JTable table, List<Reservation> reservations) {
            this.reservations = reservations;
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            panel.setOpaque(false);
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.removeAll();
            int modelRow = table.convertRowIndexToModel(row);
            Object idObj = table.getModel().getValueAt(modelRow, table.getColumnCount() - 1);
            Long resId = idObj instanceof Long ? (Long) idObj : null;

            JButton cancelBtn = new JButton("取消预约");
            styleButton(cancelBtn, DANGER, false);
            cancelBtn.addActionListener(ev -> {
                fireEditingStopped();
                int opt = JOptionPane.showConfirmDialog(frame, "确认取消该预约？", "取消预约", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    try {
                        ctx.getReservationService().cancelReservation(resId, currentUser.getUserId(), currentUser);
                        showToast("预约已取消"); refreshReservations();
                    } catch (Exception ex) { showError(ex.getMessage()); }
                }
            });
            panel.add(cancelBtn);
            return panel;
        }
        public Object getCellEditorValue() { return null; }
    }

    // ==================== 辅助查找 ====================

    private Book findBook(Long bookId) {
        return bookId == null ? null : ctx.getBookDao().findById(bookId);
    }

    private User findUser(Long userId) {
        return userId == null ? null : ctx.getUserDao().findById(userId);
    }

    // ==================== 消息提示 ====================

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void showToast(String msg) {
        statusLabel.setText("  " + msg);
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3000, e -> statusLabel.setText("就绪"));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    // ==================== 启动 ====================

    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}
