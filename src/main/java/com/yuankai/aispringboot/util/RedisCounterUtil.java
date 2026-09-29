package com.yuankai.aispringboot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 计数与原子操作公共工具类。
 * 抽取自登录限流（UserService）与文章阅读量（KnowledgeCategoryService）中重复的 INCR/GET/DELETE 逻辑，
 * 依赖：StringRedisTemplate（Spring Boot 自动配置，值序列化器为 String，读写都是字符串）。
 */
@Component
public class RedisCounterUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 原子自增（INCR），返回自增后的值。
     * 阅读量增量、失败计数都靠它：Redis 单线程保证并发下不丢失更新。
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 原子自增 + 刷新过期时间（每次操作都把 TTL 重置）。
     * 登录限流专用语义：每失败一次计数 +1，同时把 15 分钟锁定期往后刷新。
     */
    public Long incrementWithExpire(String key, long timeout, TimeUnit unit) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, timeout, unit);
        return count;
    }

    /**
     * 读取字符串值；key 不存在返回 null。
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 key（登录成功后清除失败计数、定时任务刷库后清除增量）。
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 按模式扫描 key（如 "article:read:*"）。生产环境数据量大时应改用 SCAN，本项目规模 keys 足够。
     */
    public Set<String> keys(String pattern) {
        return stringRedisTemplate.keys(pattern);
    }

    /**
     * 原子"取出并删除"：
     * 取值 + 删 key 一步完成，期间新写入的值不受影响，适合"取走增量并清零"的刷库场景。
     */
    public String getAndDelete(String key) {
        return stringRedisTemplate.opsForValue().getAndDelete(key);
    }
}
