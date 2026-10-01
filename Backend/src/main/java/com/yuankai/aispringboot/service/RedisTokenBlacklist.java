package com.yuankai.aispringboot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis Token 黑名单服务
 * 用于将已登出的 Token 加入黑名单，实现 JWT 的服务端失效机制
 */
@Service
public class RedisTokenBlacklist {
    private static final Logger log = LoggerFactory.getLogger(RedisTokenBlacklist.class);

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将 Token 加入黑名单
     * fail-open 语义：Redis 不可用时记录 WARN 并继续（登出接口仍返回成功），
     * 代价是"Redis 恢复前，该 token 暂时仍可访问"，属于可接受的短窗口。
     * @param token JWT Token
     * @param expireMillis Token 剩余有效时间（毫秒）
     */
    public void addToBlacklist(String token, long expireMillis) {
        if (expireMillis <= 0) {
            return;
        }
        String key = BLACKLIST_PREFIX + token;
        try {
            redisTemplate.opsForValue().set(key, "1", expireMillis, TimeUnit.MILLISECONDS);
            log.info("Token 已加入黑名单，有效期 {} 毫秒", expireMillis);
        } catch (Exception e) {
            log.warn("Token 加入黑名单失败（Redis 不可用？），fail-open 放行登出，token 在 Redis 恢复前暂未失效", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     * fail-open 语义：Redis 不可用时返回 false（视为未拉黑、放行），保证 Redis 故障时服务仍可用。
     * @param token JWT Token
     * @return true 表示 Token 已被拉黑
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("黑名单查询失败（Redis 不可用？），fail-open 放行 token", e);
            return false;
        }
    }
}
