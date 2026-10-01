# 心理健康助手 · AI 全栈项目

基于 **Spring Boot 4 + Spring AI** 的心理健康管理系统，覆盖用户鉴权、AI 心理咨询（流式 SSE 对话）、情绪日记、知识库、文件上传、数据分析六大模块。

> 个人学习项目，后端已完成，前端开发中。

---

## 目录导航

| 目录 | 状态 | 说明 |
|---|---|---|
| [`Backend/`](./Backend) | 已完成 | Spring Boot 后端服务，22 个接口全部落地，含完整技术文档 |
| [`Frontend/`](./Frontend) | 开发中 | Vue 3 前端工程 |

后端的详细设计、接口清单、技术亮点与启动方式见 [`Backend/README.md`](./Backend/README.md)。

---

## 技术栈

| 分类 | 技术 |
|---|---|
| 框架 | Spring Boot 4.1.0、Spring MVC |
| ORM | MyBatis-Plus 3.5.17（分页插件 / 条件构造器） |
| AI | Spring AI 2.0.1（硅基流动 Qwen2.5，流式 SSE 对话） |
| 鉴权 | JWT + 自定义过滤器 + Redis Token 黑名单登出 |
| 数据库 | MySQL 8.0、Redis 7.x |
| 部署 | Docker、Docker Compose |

---

## 功能模块

| 模块 | 关键能力 |
|---|---|
| 用户 | 注册 / 登录 / 登出，BCrypt 加密，Redis 计数防暴力破解 |
| AI 心理咨询 | 多轮会话管理，Spring AI 流式 SSE 输出，情绪分析 |
| 情绪日记 | 按 `user_id + diary_date` upsert，每天一篇 |
| 知识库 | 分类树，文章 CRUD，阅读量 Redis INCR + 定时刷库 |
| 文件上传 | 扩展名白名单 + 文件头魔数校验 |
| 数据分析 | 多表聚合概览，今日活跃用 Redis HyperLogLog 去重计数 |

---

## Redis 的五种用法

项目中 Redis 不只是缓存，按场景分了五类用法：

| 场景 | 数据结构 / 方案 |
|---|---|
| Token 登出黑名单 | String + TTL |
| 登录防暴力破解 | String 计数器，5 次失败锁 15 分钟 |
| 分类树缓存 | Cache Aside，TTL 1 小时 |
| 文章阅读量 | INCR 原子自增 + 定时任务 GETDEL 刷回 MySQL |
| 今日活跃用户 | HyperLogLog，PFADD 埋点 + PFCOUNT 去重 |

所有 Redis 操作收口在 `RedisCounterUtil` 公共工具类，且统一做 fail-open 降级——Redis 不可用时系统降级运行而非直接报错。

---

## 快速开始

```bash
cd Backend
mvn spring-boot:run
```

需要先启动 MySQL 与 Redis，并配置环境变量 `MYSQL_PASSWORD`、`JWT_SECRET`、`SILICONFLOW_API_KEY`。
完整步骤（含 Docker 一键编排）见 [`Backend/README.md`](./Backend/README.md)。
