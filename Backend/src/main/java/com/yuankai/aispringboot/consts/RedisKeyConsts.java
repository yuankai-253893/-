package com.yuankai.aispringboot.consts;

/**
 * Redis Key 常量集中管理。
 *
 * 注意：key 的"值"一经上线就不要改，否则旧缓存数据将无法命中（本项目数据均为短时效，影响可控）。
 */
public final class RedisKeyConsts {

    private RedisKeyConsts() {
    }

    /** JWT 登出黑名单前缀，完整 key = token:blacklist:{token} */
    public static final String BLACKLIST_PREFIX = "token:blacklist:";

    /** 登录失败次数计数前缀，完整 key = login:fail:{username}（防暴力破解） */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";

    /** 今日活跃用户（HyperLogLog），完整 key = active:user:{yyyy-MM-dd}，按天一个、TTL 3 天 */
    public static final String ACTIVE_KEY_PREFIX = "active:user:";

    /** 知识库分类树缓存键（全量缓存，TTL 1 小时） */
    public static final String CATEGORY_TREE_KEY = "knowledge:category:tree";

    /** 文章阅读量增量缓存前缀，完整 key = article:read:{articleId}（INCR 原子自增，定时刷库） */
    public static final String ARTICLE_READ_KEY_PREFIX = "article:read:";
}
