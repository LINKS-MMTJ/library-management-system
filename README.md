# 图书管理系统 v2.0

Vue 3 + REST API 图书管理系统，支持图书管理、借阅归还、预约排队、罚金计算、消息通知。

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 (Composition API) + Vue Router + Pinia + Axios + Vite |
| 后端 | Java 21（纯标准库，零外部依赖）+ JDK 内置 HttpServer |
| 数据 | 文件序列化存储（`library_data/` 目录，原子写保证数据安全） |
| 安全 | PBKDF2WithHmacSHA256 密码哈希 + Token 会话认证 + 权限分级 |
| 测试 | 自定义测试框架，120 个用例，黑盒（等价类+边界值）+ 白盒（语句+路径覆盖） |

## 项目结构

```
├── frontend/                  # Vue 3 前端
│   ├── src/
│   │   ├── views/            # 页面组件（9 个功能页面）
│   │   ├── components/       # 布局组件
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── api/              # Axios 接口封装（含拦截器）
│   │   └── router/           # 路由配置（含权限守卫）
│   └── vite.config.js        # Vite 配置（含 /api 代理）
├── lib/com/lib/demo/          # Java 后端源码
│   ├── api/                  # REST API 控制器（8 个）
│   ├── entity/               # 实体类（5 个）
│   ├── service/              # 业务逻辑层（5 个）
│   ├── dao/                  # 数据访问层（含 AbstractDao + FileStore）
│   ├── util/                 # 工具类（JsonUtil, PasswordUtil, LogUtil）
│   ├── exception/            # 自定义异常
│   ├── AppContext.java       # 依赖注入容器（工厂方法）
│   ├── AppConfig.java        # 全局配置（支持系统属性覆盖）
│   ├── DataSeeder.java       # 测试数据初始化器
│   └── Main.java             # 应用入口
├── test/com/lib/demo/test/   # 单元测试（7 个文件，120 个用例）
└── library_data/             # 数据文件（运行时自动生成）
```

## 快速启动

### 1. 启动后端（Java API）

```bash
# 编译（一次性编译所有源码）
javac -encoding UTF-8 -cp lib -d out lib/com/lib/demo/Main.java

# 运行（默认端口 8080）
java -cp out com.lib.demo.Main

# 指定端口（方式一：命令行参数）
java -cp out com.lib.demo.Main 9090

# 指定端口（方式二：系统属性，优先级更高）
java -Dserver.port=9090 -cp out com.lib.demo.Main

# 自定义数据目录
java -Ddata.dir=/path/to/data -cp out com.lib.demo.Main
```

启动成功后显示：

```
===================================
  图书管理系统 API 已启动
  http://localhost:8080
  http://localhost:8080/api/   (REST API)
===================================
```

### 2. 启动前端（Vue 3）

```bash
cd frontend
npm install        # 首次运行需安装依赖
npm run dev        # 启动开发服务器（端口 3000，自动代理 /api 到 8080）
```

### 3. 打开浏览器

访问 **http://localhost:3000**

> **生产模式**：先 `npm run build` 构建前端，再启动 Java 后端，后端会自动托管 `frontend/dist/` 静态文件。此时只需访问 **http://localhost:8080** 即可（前后端同端口）。

## 配置参数

所有可调参数通过系统属性（`-D`）设置：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | `8080` | API 服务器端口（也可通过命令行参数传入） |
| `data.dir` | `library_data` | 数据文件存储目录 |
| `token.ttl` | `86400000` | Token 有效期（毫秒），默认 24 小时 |

## 默认测试账号

| 用户名 | 密码 | 角色 | 权限 |
|--------|------|------|------|
| `admin` | `admin123` | 系统管理员 | 全部功能 |
| `lib1` | `lib123` | 图书管理员 | 图书/借阅/预约管理 |
| `user1` | `user123` | 借阅者 | 浏览/借阅/我的借阅 |

## 运行测试

```bash
# 编译测试
javac -encoding UTF-8 -cp "lib;test" -d out test/com/lib/demo/test/AllTests.java

# 运行全部测试（120 个用例）
java -cp "out;lib;test" com.lib.demo.test.AllTests

# 单独运行某个模块
java -cp "out;lib;test" com.lib.demo.test.BookServiceTest
java -cp "out;lib;test" com.lib.demo.test.UserServiceTest
```

## API 接口一览

### 认证
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 公开 |
| POST | `/api/auth/register` | 注册 | 公开 |
| GET | `/api/auth/me` | 当前用户信息 | 登录 |

### 图书管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/books` | 图书列表（支持 `?keyword=` 搜索） | 登录 |
| POST | `/api/books` | 新书上架 | 管理员 |
| PUT | `/api/books/:id` | 编辑图书 | 管理员 |
| DELETE | `/api/books/:id` | 下架图书 | 管理员 |
| POST | `/api/books/:id/borrow` | 借阅 | 借阅者 |
| POST | `/api/books/:id/reserve` | 预约 | 借阅者 |

### 用户管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/users` | 用户列表 | 管理员 |
| POST | `/api/users` | 创建用户 | 管理员 |
| PUT | `/api/users/:id` | 编辑用户 | 管理员 |
| DELETE | `/api/users/:id` | 删除用户 | 管理员 |
| PUT | `/api/users/:id/disable` | 禁用用户 | 管理员 |
| PUT | `/api/users/:id/enable` | 启用用户 | 管理员 |
| PUT | `/api/users/:id/pay-fine` | 缴纳罚金 | 本人 |

### 借阅管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/borrows/my` | 我的借阅 | 登录 |
| GET | `/api/borrows` | 全部借阅 | 管理员 |
| POST | `/api/borrows/:id/return` | 归还图书 | 借阅者 |
| POST | `/api/borrows/:id/renew` | 续借 | 借阅者 |

### 预约 & 通知 & 系统
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/reservations` | 预约列表 | 登录 |
| POST | `/api/reservations/:id/cancel` | 取消预约 | 预约人 |
| GET | `/api/notifications` | 消息通知 | 登录 |
| PUT | `/api/notifications/:id/read` | 标记已读 | 本人 |
| GET | `/api/dashboard` | 仪表盘数据 | 登录 |
| POST | `/api/system/overdue-reminders` | 发送逾期提醒 | 管理员 |
| POST | `/api/system/out-of-stock` | 发送缺货通知 | 管理员 |

## 核心业务规则

| 规则 | 说明 |
|------|------|
| 借阅权限 | 仅借阅者可借书；须无逾期记录、无未缴罚金、账户未被禁用 |
| 借阅期限 | 30 天，续借最多 2 次，每次延长 15 天（被他人预约时不可续借） |
| 罚金计算 | 逾期每天 ¥0.50，金额以**分**为单位存储（`long`），避免浮点误差 |
| 预约排队 | 库存为零时可预约，归还/取消后自动通知下一排队者 |
| 缺货通知 | 预约等待超 30 天自动发送缺货通知，抄送管理员 |
| 密码安全 | PBKDF2WithHmacSHA256 + 16 字节随机盐 + 10 万次迭代 |
| 认证机制 | Token（UUID）认证，24 小时过期，定期清理无效会话 |
| 数据安全 | 文件采用原子写（临时文件 → rename），避免写入中断导致数据损坏 |
| API 安全 | 三层权限校验（公开/登录/管理员），API 响应不含密码字段 |

## 许可证

[MIT License](LICENSE)
