# 心理健康助手 · 后端服务

基于 Spring Boot + MyBatis-Plus + Spring AI 的心理健康管理系统后端，覆盖用户、AI 心理咨询、情绪日记、知识库、文件上传、数据分析六大模块。

---

## 技术栈

| 分类 | 技术 |
|---|---|
| 框架 | Spring Boot 4.1.0、Spring MVC |
| ORM | MyBatis-Plus 3.5.17（分页插件 / 条件构造器 / 原生 `@Select` 聚合） |
| AI | Spring AI 2.0.1（硅基流动 Qwen2.5，流式 SSE 对话） |
| 鉴权 | java-jwt 4.4.0（自定义 JWT 过滤器 + Redis Token 黑名单登出） |
| 数据库 | MySQL 8.0、Redis（Token 黑名单） |
| 工具 | Lombok、Hutool、Jakarta Validation |
| 构建 | Maven、Java 17+ |

---

## 功能模块与接口

### 1. 用户模块
| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/user/add` | POST | 注册（仅允许普通用户，注册接口封闭提权漏洞） |
| `/api/user/login` | POST | 登录，返回 JWT |
| `/api/user/current` | GET | 获取当前登录用户 |
| `/api/user/logout` | POST | 登出（Redis Token 黑名单） |

### 2. AI 心理咨询
| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/psychological-chat/session/start` | POST | 创建咨询会话 |
| `/api/psychological-chat/stream` | POST | 流式 SSE 对话（Spring AI） |
| `/api/psychological-chat/sessions` | GET | 分页查询会话 |
| `/api/psychological-chat/sessions/{sessionId}/messages` | GET | 会话消息列表 |
| `/api/psychological-chat/sessions/{sessionId}` | DELETE | 删除会话 |
| `/api/psychological-chat/session/{sessionId}/emotion` | GET | 会话情绪分析结果 |

### 3. 情绪日记
| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/emotion-diary` | POST | 创建/更新（按 `user_id + diary_date` upsert，每天一篇） |
| `/api/emotion-diary/admin/page` | GET | 管理端分页 |
| `/api/emotion-diary/admin/{id}` | DELETE | 删除 |

### 4. 知识库
| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/knowledge/category/tree` | GET | 分类树（一次查全 → 内存挂树） |
| `/api/knowledge/article/page` | GET | 文章分页（用户端按阅读量排序，排序字段白名单防注入） |
| `/api/knowledge/admin/article/page` | GET | 管理端文章分页 |
| `/api/knowledge/article` | POST | 新增文章（UUID 主键） |
| `/api/knowledge/article/{id}` | GET | 详情（阅读量 SQL 原子自增） |
| `/api/knowledge/article/{id}` | PUT | 更新 |
| `/api/knowledge/article/{id}/status` | PUT | 发布/下线 |
| `/api/knowledge/article/{id}` | DELETE | 删除 |

### 5. 文件上传
| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/file/upload` | POST | 图片/文档上传（扩展名白名单 + 大小限制） |

### 6. 数据分析
| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/data-analytics/overview` | GET | 管理端概览：用户数 / 今日活跃 / 日记均值 / 近 7 日情绪趋势（多表聚合） |

> 需鉴权接口统一用请求头 `token` 传递 JWT；管理端接口统一 `/admin` 前缀 + 角色校验。

---

## 项目结构

```
src/main/java/com/yuankai/aispringboot
├── controller/     # 接口层（登录鉴权后调用 Service）
├── service/        # 业务层
├── mapper/         # MyBatis-Plus Mapper
├── entity/         # 数据库实体
├── DTO/
│   ├── command/    # 入参（带校验注解）
│   ├── query/      # 查询条件
│   └── response/   # 出参
├── config/         # WebConfig 静态映射、MyBatis-Plus 分页插件
├── common/         # Result / ResultCode 统一返回
├── exception/      # BusinessException + 全局异常处理
├── enumclass/      # UserType / UserStatus 枚举
└── util/           # JWT 工具、GetUserInfo
```

## 数据库

9 张表：`user`、`consultation_session`、`consultation_message`、`emotion_diary`、`knowledge_category`、`knowledge_article`、`sys_file_info`、`user_favorite`、`ai_analysis_task`。建表脚本见根目录 `mental_health_assistant.sql`。

---

## 快速启动

**环境要求**：JDK 17+、MySQL 8、Redis（可选，不装也能跑，仅登出黑名单用 Redis）。

1. 导入数据库：`mysql -u root -p < mental_health_assistant.sql`
2. 配置环境变量：

   ```powershell
   $env:MYSQL_PASSWORD='你的MySQL密码'
   $env:JWT_SECRET='自定义JWT密钥'
   $env:SILICONFLOW_API_KEY='硅基流动API Key'   # AI 对话用，可填占位
   ```

3. 启动：`mvn spring-boot:run`，默认端口 `8080`
4. 本地存储目录：`file.upload-path`（`application.yml`，上传文件落盘根目录）

---

## 技术亮点

- **权限三层防线**：JWT 过滤器（登录）→ Controller 角色校验（管理端 `/admin`）→ Service 存在性/业务校验（防越权拼 id 访问未发布文章）
- **防注入**：排序字段白名单映射，用户输入不直接拼 SQL
- **并发安全**：文章阅读量用 SQL 原子自增 `read_count = read_count + 1`，避免读改写丢失更新
- **文件上传安全**：扩展名白名单（拒绝 .exe/.jsp/.html）、服务端重命名防路径穿越、大小限制
- **统一异常处理**：`BusinessException` + 全局处理器，参数校验/业务异常/系统异常分级返回
- **聚合统计**：今日活跃用 `UNION` 去重业务行为近似（user 表无最后登录时间字段的口径设计）

---

## 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 管理员 |
| test | 123456 | 普通用户 |
