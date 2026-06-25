package com.lib.demo.service;

import com.lib.demo.dao.UserDao;
import com.lib.demo.entity.User;
import com.lib.demo.exception.BusinessException;
import com.lib.demo.util.LogUtil;
import com.lib.demo.util.PasswordUtil;

import java.util.List;
import java.util.logging.Logger;

public class UserService {
    private static final Logger LOG = LogUtil.getLogger(UserService.class);
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User register(String username, String password, String name, String email, String phone) {
        if (username == null || username.trim().isEmpty()) throw new BusinessException("用户名不能为空");
        if (password == null || password.trim().isEmpty()) throw new BusinessException("密码不能为空");
        if (password.length() < 6) throw new BusinessException("密码至少需要6位");  // B7修复
        if (name == null || name.trim().isEmpty()) throw new BusinessException("真实姓名不能为空");
        if (userDao.usernameExists(username)) throw new BusinessException("用户名已存在");

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.hash(password));
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(User.Role.BORROWER);
        user.setStatus(User.Status.ACTIVE);
        user.setUnpaidFine(0);
        userDao.save(user);
        LOG.info("新用户注册: " + username);
        return user;
    }

    public User login(String username, String password) {
        if (username == null || password == null) return null;
        User user = userDao.findByUsername(username);
        if (user != null && PasswordUtil.verify(password, user.getPassword()) && user.isActive()) {
            LOG.info("用户登录: " + username + " (" + user.getRole().getDescription() + ")");
            return user;
        }
        return null;
    }

    // ==================== 管理员用户管理 ====================

    public List<User> getAllUsers(User operator) {
        if (!operator.isAdmin()) throw new BusinessException("权限不足：仅系统管理员可执行此操作");
        return userDao.findAll();
    }

    public User createUser(String username, String password, String name, User.Role role,
                           User.Status status, String email, String phone, User operator) {
        if (!operator.isAdmin()) throw new BusinessException("权限不足：仅系统管理员可执行此操作");
        if (username == null || username.trim().isEmpty()) throw new BusinessException("用户名不能为空");
        if (password == null || password.trim().isEmpty()) throw new BusinessException("密码不能为空");
        if (password.length() < 6) throw new BusinessException("密码至少需要6位");  // B7修复
        if (userDao.usernameExists(username)) throw new BusinessException("用户名已存在");

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.hash(password));
        user.setName(name);
        user.setRole(role);
        user.setStatus(status);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUnpaidFine(0);
        userDao.save(user);
        LOG.info("管理员 " + operator.getUsername() + " 创建用户: " + username + " (角色: " + role.getDescription() + ")");
        return user;
    }

    public User updateUser(Long userId, User updatedInfo, User operator) {
        if (!operator.isAdmin()) throw new BusinessException("权限不足：仅系统管理员可执行此操作");
        User user = userDao.findById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (updatedInfo.getUsername() != null) {
            // B3修复：检查新用户名是否已存在
            String newUsername = updatedInfo.getUsername();
            if (!newUsername.equals(user.getUsername()) && userDao.usernameExists(newUsername)) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(newUsername);
        }
        if (updatedInfo.getPassword() != null) user.setPassword(PasswordUtil.hash(updatedInfo.getPassword()));
        if (updatedInfo.getName() != null) user.setName(updatedInfo.getName());
        if (updatedInfo.getRole() != null) user.setRole(updatedInfo.getRole());
        if (updatedInfo.getStatus() != null) user.setStatus(updatedInfo.getStatus());
        if (updatedInfo.getEmail() != null) user.setEmail(updatedInfo.getEmail());
        if (updatedInfo.getPhone() != null) user.setPhone(updatedInfo.getPhone());

        userDao.update(user);
        LOG.info("管理员 " + operator.getUsername() + " 更新用户: " + user.getUsername());
        return user;
    }

    public void deleteUser(Long userId, User operator) {
        if (!operator.isAdmin()) throw new BusinessException("权限不足：仅系统管理员可执行此操作");
        if (operator.getUserId().equals(userId)) throw new BusinessException("不能删除自己");
        userDao.delete(userId);
        LOG.info("管理员 " + operator.getUsername() + " 删除用户: ID=" + userId);
    }

    public void disableUser(Long userId, User operator) {
        if (!operator.isAdmin()) throw new BusinessException("权限不足：仅系统管理员可执行此操作");
        if (operator.getUserId().equals(userId)) throw new BusinessException("不能禁用自己");
        User user = userDao.findById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(User.Status.INACTIVE);
        userDao.update(user);
        LOG.info("管理员 " + operator.getUsername() + " 禁用用户: " + user.getUsername());
    }

    public void enableUser(Long userId, User operator) {
        if (!operator.isAdmin()) throw new BusinessException("权限不足：仅系统管理员可执行此操作");
        User user = userDao.findById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(User.Status.ACTIVE);
        userDao.update(user);
        LOG.info("管理员 " + operator.getUsername() + " 启用用户: " + user.getUsername());
    }

    // ==================== 罚金管理 ====================

    public User payFine(Long userId, long amountCents) {
        User user = userDao.findById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (user.getUnpaidFine() <= 0) throw new BusinessException("没有待缴纳的罚金");
        if (amountCents <= 0) throw new BusinessException("缴纳金额必须大于0");
        if (amountCents > user.getUnpaidFine()) throw new BusinessException("缴纳金额超过欠款");

        user.setUnpaidFine(user.getUnpaidFine() - amountCents);
        userDao.update(user);
        LOG.info("用户 " + user.getUsername() + " 缴纳罚金: ¥" + centsStr(amountCents) +
                "，剩余: ¥" + centsStr(user.getUnpaidFine()));
        return user;
    }

    public User getUserById(Long userId) {
        return userDao.findById(userId);
    }

    public void updateUser(User user) {
        userDao.update(user);
    }

    private static String centsStr(long cents) {
        return String.format("%.2f", cents / 100.0);
    }
}
