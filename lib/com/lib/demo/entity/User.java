package com.lib.demo.entity;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMIN("系统管理员"),
        LIBRARIAN("图书管理员"),
        BORROWER("借阅者");

        private final String description;
        Role(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum Status {
        ACTIVE, INACTIVE, LOCKED, PENDING
    }

    private Long userId;
    private String username;
    private String password;
    private Role role;
    private String name;
    private String email;
    private String phone;
    private Status status;
    private double unpaidFine;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public double getUnpaidFine() { return unpaidFine; }
    public void setUnpaidFine(double unpaidFine) { this.unpaidFine = unpaidFine; }

    public boolean isAdmin() { return Role.ADMIN.equals(this.role); }
    public boolean isLibrarian() { return Role.LIBRARIAN.equals(this.role); }
    public boolean isBorrower() { return Role.BORROWER.equals(this.role); }
    public boolean isActive() { return Status.ACTIVE.equals(this.status); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId != null && userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "', role=" +
                (role != null ? role.name() : "null") + ", name='" + name + "', status=" +
                (status != null ? status.name() : "null") + "}";
    }
}
