package com.lib.demo.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static com.lib.demo.ui.UIConstants.FONT_SMALL;

/**
 * 通用按钮列渲染器 — 提示文字"点击操作"。
 */
class ButtonRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    public ButtonRenderer() {
        setFont(FONT_SMALL);
        setForeground(new Color(148, 163, 184));
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setText("点击操作");
        setBackground(isSelected ? new Color(219, 234, 254) : Color.WHITE);
        return this;
    }
}
