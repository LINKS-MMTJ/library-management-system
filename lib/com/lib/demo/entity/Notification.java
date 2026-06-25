package com.lib.demo.entity;

import java.io.Serializable;
import java.time.LocalDate;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        FINE("罚金通知"),
        OVERDUE_REMINDER("逾期提醒"),
        RESERVATION_SUCCESS("预定成功"),
        BOOK_AVAILABLE("图书可借"),
        OUT_OF_STOCK("缺书通知"),
        SYSTEM("系统通知");

        private final String description;
        Type(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private Long notificationId;
    private Long userId;
    private String content;
    private Type type;
    private LocalDate sendTime;
    private boolean read;

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public LocalDate getSendTime() { return sendTime; }
    public void setSendTime(LocalDate sendTime) { this.sendTime = sendTime; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public String toString() {
        return "Notification{id=" + notificationId + ", userId=" + userId +
                ", type=" + type + ", read=" + read + "}";
    }
}
