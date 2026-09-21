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
     * @param token JWT Token
     * @param expireMillis Token 剩余有效时间（毫秒）
     */
    public void addToBlacklist(String token, long expireMillis) {
        if (expireMillis <= 0) {
            return;
        }
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expireMillis, TimeUnit.MILLISECONDS);
        log.info("Token 已加入黑名单，有效期 {} 毫秒", expireMillis);
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param token JWT Token
     * @return true 表示 Token 已被拉黑
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
