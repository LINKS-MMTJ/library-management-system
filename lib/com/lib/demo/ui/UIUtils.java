package com.lib.demo.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.lib.demo.ui.UIConstants.*;

/**
 * UI 工具类：按钮样式、表格创建、渲染器、Toast 等共用方法。
 */
final class UIUtils {

    private UIUtils() {}

    // ── 按钮样式 ──

    static void styleButton(JButton btn, Color bg, boolean block) {
        btn.setBackground(bg);
        btn.setForeground(isLightColor(bg) ? TEXT_DARK : Color.WHITE);
        btn.setFont(FONT_SMALL);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(darken(bg, 10), 1, true),
                new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(darken(bg, 20)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
    }

    static boolean isLightColor(Color c) {
        double luminance = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        return luminance > 0.3;
    }

    static Color darken(Color c, int amount) {
        return new Color(
                Math.max(0, c.getRed() - amount),
                Math.max(0, c.getGreen() - amount),
                Math.max(0, c.getBlue() - amount));
    }

    // ── 表格 ──

    static JTable createStyledTable(String[] cols, Object[][] data) {
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            public boolean isCellEditable(int row, int col) {
                return col == getColumnCount() - 1; // 只有操作列可编辑
            }
        };
        JTable table = new JTable(model) {
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isCellSelected(row, col)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_STRIPE);
                }
                return c;
            }
        };
        table.setFont(FONT_NORMAL);
        table.setRowHeight(34);
        table.getTableHeader().setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(TEXT_DARK);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_LIGHT));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(TEXT_DARK);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.putClientProperty("JTable.autoStartsEdit", Boolean.TRUE); // 单击进入编辑
        return table;
    }

    static void hideColumn(JTable table, int col) {
        table.getColumnModel().getColumn(col).setMinWidth(0);
        table.getColumnModel().getColumn(col).setMaxWidth(0);
        table.getColumnModel().getColumn(col).setWidth(0);
    }

    // ── 渲染器 ──

    static DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int row, int col) {
                String s = String.valueOf(v);
                setText(s);
                setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
                setHorizontalAlignment(SwingConstants.CENTER);
                Color bg, fg;
                if ("已逾期".equals(s)) {
                    fg = new Color(153, 27, 27);
                    bg = new Color(254, 226, 226);
                } else if ("正常".equals(s) || "借阅中".equals(s)) {
                    fg = new Color(6, 95, 70);
                    bg = new Color(209, 250, 229);
                } else if ("已归还".equals(s)) {
                    fg = new Color(71, 85, 105);
                    bg = new Color(241, 245, 249);
                } else {
                    fg = TEXT_DARK; bg = Color.WHITE;
                }
                if (sel) bg = new Color(199, 224, 255);
                setForeground(fg);
                setBackground(bg);
                return this;
            }
        };
    }

    // ── 面板组件 ──

    static JPanel createCardPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_LIGHT, 1, true),
                new EmptyBorder(12, 12, 12, 12)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        return panel;
    }

    static JPanel createTablePanel(String title, String[] cols, Object[][] data) {
        JPanel panel = createCardPanel(title);
        if (data.length == 0) {
            panel.add(createEmptyLabel("暂无数据"));
        } else {
            JTable table = createStyledTable(cols, data);
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
            panel.add(sp, BorderLayout.CENTER);
        }
        return panel;
    }

    static JLabel createEmptyLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        label.setForeground(new Color(148, 163, 184));
        label.setBorder(new EmptyBorder(48, 10, 48, 10));
        return label;
    }

    static JPanel createFormRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_NORMAL);
        lbl.setPreferredSize(new Dimension(130, 28));
        field.setFont(FONT_NORMAL);
        field.setPreferredSize(new Dimension(250, 28));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.add(Box.createVerticalStrut(28));
        return row;
    }

    // ── 工具方法 ──

    static String getOpt(JTextField field) {
        String s = field.getText().trim();
        return s.isEmpty() ? null : s;
    }

    static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
