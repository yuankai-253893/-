package com.yuankai.aispringboot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 计数与原子操作公共工具类。
 * 登录限流（UserService）与文章阅读量（KnowledgeCategoryService），
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

    /**
     * 设置 key 的过期时间（幂等，可重复调用刷新 TTL）。
     * 登录限流的 incrementWithExpire、活跃埋点的 expire 都依赖它。
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * HyperLogLog 添加元素（PFADD）。
     * 用于今日活跃用户去重计数：内存固定 12KB 左右，可统计 2^64 个元素，误差约 0.81%。
     * 返回值 1 表示基数可能发生了变化，0 表示元素已存在（本方法返回值仅作参考，业务一般用 pfCount）。
     */
    public Long pfAdd(String key, String... values) {
        return stringRedisTemplate.opsForHyperLogLog().add(key, values);
    }

    /**
     * HyperLogLog 基数估算（PFCOUNT）。
     * 返回该 key 下去重后的元素个数估算值；key 不存在返回 0。
     */
    public Long pfCount(String key) {
        return stringRedisTemplate.opsForHyperLogLog().size(key);
    }
}
