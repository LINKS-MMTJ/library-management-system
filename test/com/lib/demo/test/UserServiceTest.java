package com.lib.demo.test;

import com.lib.demo.entity.User;
import com.lib.demo.exception.BusinessException;

/**
 * 用户模块测试 — 覆盖 UserService 全部方法
 *
 * 黑盒设计方法:
 *   等价类划分: 用户名(有效/空/已存在), 密码(>=6/<6/空), 角色(三种), 状态(ACTIVE/INACTIVE)
 *   边界值分析: 密码长度=5/6/7, 罚金金额=0/0.01/负数
 *
 * 白盒覆盖目标:
 *   语句覆盖: 每个方法的所有可执行语句
 *   路径覆盖: 每个 if/else 分支, 每个 throw 路径
 */
public class UserServiceTest extends TestBase {

    public static void main(String[] args) {
        UserServiceTest t = new UserServiceTest();
        t.runAll();
    }

    public void runAll() {
        startModule("用户管理 (UserService)");
        initModuleStats();

        testRegister();             // 黑盒: 等价类 + 边界值
        testLogin();                // 黑盒: 等价类
        testCreateUser();           // 白盒: 权限 + 参数校验路径
        testUpdateUser();           // 白盒: 全字段更新 + 重名检测
        testDeleteUser();           // 白盒: 自我保护 + 欠款/未还检测
        testDisableEnableUser();    // 黑盒: 状态切换
        testPayFine();              // 黑盒: 边界值

        printSummary();
    }

    // ==================== 黑盒测试: 注册 (等价类 + 边界值) ====================

    private void testRegister() {
        System.out.println("\n  ▸ 黑盒测试: 注册 (等价类划分 + 边界值分析)");

        // --- 有效等价类 ---
        test("注册-有效输入(全部字段填满)", () -> {
            setUp(); User u = ctx.getUserService().register("newuser", "pass123", "测试人",
                    "test@test.com", "13800138000");
            assertNotNull(u, "注册应成功返回User对象");
            assertEquals("newuser", u.getUsername(), null);
            assertEquals(User.Role.BORROWER, u.getRole(), null);
        });

        test("注册-有效输入(仅必填字段)", () -> {
            setUp(); User u = ctx.getUserService().register("minuser", "123456", "最小人", null, null);
            assertNotNull(u, "仅填必填项应注册成功");
        });

        // --- 无效等价类: 用户名为空 ---
        test("注册-用户名为null", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register(null, "123456", "张三", null, null),
                    "用户名为null应抛异常");
        });

        test("注册-用户名为空串", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register("  ", "123456", "张三", null, null),
                    "用户名为空串应抛异常");
        });

        // --- 无效等价类: 密码问题 ---
        test("注册-密码为null", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register("u1", null, "张三", null, null),
                    "密码为null应抛异常");
        });

        test("注册-密码为空串", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register("u1", "  ", "张三", null, null),
                    "密码为空串应抛异常");
        });

        // --- 边界值: 密码长度 ---
        test("注册-边界值: 密码长度=5 (无效)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register("u1", "12345", "张三", null, null),
                    "密码<6位应抛异常");
        });

        test("注册-边界值: 密码长度=6 (有效)", () -> {
            setUp(); User u = ctx.getUserService().register("u2", "123456", "李四", null, null);
            assertNotNull(u, "密码恰好6位应注册成功");
        });

        test("注册-边界值: 密码长度=7 (有效)", () -> {
            setUp(); User u = ctx.getUserService().register("u3", "1234567", "王五", null, null);
            assertNotNull(u, "密码>6位应注册成功");
        });

        // --- 无效等价类: 姓名为空 ---
        test("注册-姓名为空", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register("u4", "123456", "  ", null, null),
                    "姓名为空应抛异常");
        });

        // --- 无效等价类: 用户名已存在 ---
        test("注册-用户名已存在", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().register("admin", "123456", "张三", null, null),
                    "用户名重复应抛异常");
        });
    }

    // ==================== 黑盒测试: 登录 ====================

    private void testLogin() {
        System.out.println("\n  ▸ 黑盒测试: 登录 (等价类划分)");

        test("登录-正确用户名密码", () -> {
            setUp(); User u = ctx.getUserService().login("admin", "admin123");
            assertNotNull(u, "正确凭据应登录成功");
            assertEquals(User.Role.ADMIN, u.getRole(), null);
        });

        test("登录-错误密码", () -> {
            setUp(); User u = ctx.getUserService().login("admin", "wrong");
            assertNull(u, "错误密码应返回null");
        });

        test("登录-不存在的用户", () -> {
            setUp(); User u = ctx.getUserService().login("nobody", "123456");
            assertNull(u, "不存在用户应返回null");
        });

        test("登录-用户名为null", () -> {
            setUp(); User u = ctx.getUserService().login(null, "123");
            assertNull(u, "null用户名应返回null");
        });

        test("登录-密码为null", () -> {
            setUp(); User u = ctx.getUserService().login("admin", null);
            assertNull(u, "null密码应返回null");
        });
    }

    // ==================== 白盒测试: 管理员创建用户 ====================

    private void testCreateUser() {
        System.out.println("\n  ▸ 白盒测试: 创建用户 (路径覆盖)");

        // 路径: 权限不足
        test("创建用户-借阅者无权创建", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().createUser("u", "123456", "n", User.Role.BORROWER,
                            User.Status.ACTIVE, null, null, borrower),
                    "借阅者无权创建用户");
        });

        // 路径: 管理员可创建所有角色
        test("创建用户-管理员创建借阅者", () -> {
            setUp(); User u = ctx.getUserService().createUser("u1", "123456", "新人",
                    User.Role.BORROWER, User.Status.ACTIVE, "a@b.com", "138", admin);
            assertNotNull(u, "createUser返回不应为null"); assertEquals(User.Role.BORROWER, u.getRole(), null);
        });

        test("创建用户-管理员创建图书管理员", () -> {
            setUp(); User u = ctx.getUserService().createUser("lib2", "123456", "管理员2",
                    User.Role.LIBRARIAN, User.Status.ACTIVE, null, null, admin);
            assertNotNull(u, "createUser返回不应为null"); assertEquals(User.Role.LIBRARIAN, u.getRole(), null);
        });

        // 路径: 用户名为空
        test("创建用户-用户名为空(管理员操作)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().createUser("  ", "123456", "n", User.Role.BORROWER,
                            User.Status.ACTIVE, null, null, admin),
                    "用户名为空应抛异常");
        });

        // 路径: 密码<6
        test("创建用户-密码太短(管理员操作)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().createUser("u", "12345", "n", User.Role.BORROWER,
                            User.Status.ACTIVE, null, null, admin),
                    "密码<6应抛异常");
        });

        // 路径: 用户名已存在
        test("创建用户-用户名已存在(管理员操作)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().createUser("admin", "123456", "n", User.Role.BORROWER,
                            User.Status.ACTIVE, null, null, admin),
                    "重复用户名应抛异常");
        });
    }

    // ==================== 白盒测试: 更新用户 ====================

    private void testUpdateUser() {
        System.out.println("\n  ▸ 白盒测试: 更新用户 (路径覆盖)");

        test("更新用户-权限不足(借阅者)", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().updateUser(borrower.getUserId(),
                            new User(), borrower),
                    "借阅者无权更新用户");
        });

        test("更新用户-用户不存在", () -> {
            setUp(); User u = new User(); u.setName("test");
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().updateUser(99999L, u, admin),
                    "更新不存在用户应抛异常");
        });

        test("更新用户-改名不冲突", () -> {
            setUp(); User u = new User(); u.setName("新名字");
            User updated = ctx.getUserService().updateUser(borrower.getUserId(), u, admin);
            assertEquals("新名字", updated.getName(), null);
        });

        test("更新用户-改名为已存在的用户名", () -> {
            setUp(); User u = new User(); u.setUsername("admin"); // 与admin重复
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().updateUser(borrower.getUserId(), u, admin),
                    "改为已存在用户名应抛异常");
        });

        test("更新用户-全字段更新(密码/邮箱/电话)", () -> {
            setUp(); User u = new User();
            u.setName("全改"); u.setPassword("newpwd"); u.setEmail("new@mail.com");
            u.setPhone("999"); u.setRole(User.Role.LIBRARIAN); u.setStatus(User.Status.INACTIVE);
            User updated = ctx.getUserService().updateUser(borrower.getUserId(), u, admin);
            assertEquals("全改", updated.getName(), null);
            assertEquals("newpwd", updated.getPassword(), null);
            assertEquals(User.Role.LIBRARIAN, updated.getRole(), null);
            assertEquals(User.Status.INACTIVE, updated.getStatus(), null);
        });
    }

    // ==================== 白盒测试: 删除用户 ====================

    private void testDeleteUser() {
        System.out.println("\n  ▸ 白盒测试: 删除用户 (路径覆盖)");

        test("删除用户-权限不足", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().deleteUser(1L, borrower),
                    "借阅者无权删除用户");
        });

        test("删除用户-不能删除自己", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().deleteUser(admin.getUserId(), admin),
                    "不能删除自己");
        });

        test("删除用户-成功删除无借阅用户", () -> {
            setUp();
            // 先创建一个无借阅记录的用户
            User temp = ctx.getUserService().register("tempuser", "123456", "临时工", null, null);
            ctx.getUserService().deleteUser(temp.getUserId(), admin);
            assertNull(ctx.getUserDao().findById(temp.getUserId()), "用户应已删除");
        });
    }

    // ==================== 黑盒测试: 禁用/启用用户 ====================

    private void testDisableEnableUser() {
        System.out.println("\n  ▸ 黑盒测试: 禁用/启用用户 (等价类)");

        test("禁用-成功禁用活跃用户", () -> {
            setUp(); ctx.getUserService().disableUser(borrower.getUserId(), admin);
            User u = ctx.getUserDao().findById(borrower.getUserId());
            assertEquals(User.Status.INACTIVE, u.getStatus(), null);
        });

        test("禁用-不能禁用自己", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().disableUser(admin.getUserId(), admin),
                    "不能禁用自己");
        });

        test("禁用-权限不足", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().disableUser(borrower.getUserId(), borrower),
                    "借阅者无权禁用");
        });

        test("启用-成功启用已禁用用户", () -> {
            setUp(); ctx.getUserService().disableUser(borrower.getUserId(), admin);
            ctx.getUserService().enableUser(borrower.getUserId(), admin);
            assertEquals(User.Status.ACTIVE,
                    ctx.getUserDao().findById(borrower.getUserId()).getStatus(), null);
        });
    }

    // ==================== 黑盒测试: 罚金 ====================

    private void testPayFine() {
        System.out.println("\n  ▸ 黑盒测试: 缴纳罚金 (边界值分析)");

        test("缴纳-无罚金时缴费", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().payFine(borrower.getUserId(), 1.0),
                    "无罚金时缴费应抛异常");
        });

        // 边界值: 缴纳金额 <= 0
        test("缴纳-金额为0", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().payFine(borrower.getUserId(), 0),
                    "金额为0应抛异常");
        });

        test("缴纳-金额为负数", () -> {
            setUp();
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().payFine(borrower.getUserId(), -1),
                    "金额为负应抛异常");
        });

        // 需先制造一笔罚金再测
        test("缴纳-金额超过欠款", () -> {
            setUp();
            // 手动给用户加罚金
            borrower.setUnpaidFine(5.0);
            ctx.getUserDao().update(borrower);
            assertThrows(BusinessException.class,
                    () -> ctx.getUserService().payFine(borrower.getUserId(), 10.0),
                    "缴纳金额超过欠款应抛异常");
        });

        test("缴纳-精确缴纳欠款", () -> {
            setUp();
            borrower.setUnpaidFine(5.0);
            ctx.getUserDao().update(borrower);
            User u = ctx.getUserService().payFine(borrower.getUserId(), 5.0);
            assertEquals(0.0, u.getUnpaidFine(), 0.001, "应全部缴清");
        });

        test("缴纳-部分缴纳", () -> {
            setUp();
            borrower.setUnpaidFine(5.0);
            ctx.getUserDao().update(borrower);
            User u = ctx.getUserService().payFine(borrower.getUserId(), 2.0);
            assertEquals(3.0, u.getUnpaidFine(), 0.001, "应剩余3.0");
        });
    }

    // ==================== 模块统计重置 ====================

    private void initModuleStats() {
        passed = 0; failed = 0;
    }
}
