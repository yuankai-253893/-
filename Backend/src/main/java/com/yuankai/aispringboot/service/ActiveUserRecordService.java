package com.yuankai.aispringboot.service;

import com.yuankai.aispringboot.consts.RedisKeyConsts;
import com.yuankai.aispringboot.mapper.EmotionDiaryMapper;
import com.yuankai.aispringboot.util.RedisCounterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 今日活跃用户统计服务。
 *
 * 方案：Redis HyperLogLog（PFADD 埋点 + PFCOUNT 统计），替代原先"SQL UNION 去重"聚合。
 * - 埋点：用户发生活跃行为（登录 / 开启咨询会话 / 写情绪日记）时 record(userId)
 * - Key：active:user:{yyyy-MM-dd}，按天一个，TTL 3 天自动清理防堆积
 * - 统计：PFCOUNT 当天 key，内存固定约 12KB、去重自动完成、误差约 0.81%（活跃量级完全可接受）
 * - 降级：Redis 不可用时（PFADD/PFCOUNT 抛异常）自动回退 SQL 口径（今日写过日记或开过会话的用户数），
 *         保证管理端接口在 Redis 故障时依然有数据返回
 */
@Slf4j
@Service
public class ActiveUserRecordService {

    private static final long ACTIVE_KEY_TTL_DAYS = 3;

    @Autowired
    private RedisCounterUtil redisCounterUtil;

    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    /** 今天的活跃 key，如 active:user:2026-09-30 */
    private String todayKey() {
        return RedisKeyConsts.ACTIVE_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 记录一次用户活跃行为（幂等：同一用户同一天多次行为只算一人）。
     * Redis 故障时静默降级（仅记录日志），不影响用户正常使用业务功能。
     */
    public void record(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisCounterUtil.pfAdd(todayKey(), userId.toString());
            // 每次埋点刷新 TTL（3 天），防止 key 长期堆积
            redisCounterUtil.expire(todayKey(), ACTIVE_KEY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("活跃埋点失败（Redis 不可用？），降级为不记录：userId={}", userId, e);
        }
    }

    /**
     * 今日活跃用户数。
     * 优先读 Redis HyperLogLog；Redis 不可用时回退 SQL（今日写过日记或开过会话的去重人数）。
     */
    public Long countToday() {
        try {
            Long count = redisCounterUtil.pfCount(todayKey());
            log.info("今日活跃统计走 Redis PFCOUNT：key={} count={}", todayKey(), count);
            return count;
        } catch (Exception e) {
            log.warn("今日活跃统计 Redis 不可用，回退 SQL 口径", e);
            return emotionDiaryMapper.selectTodayActiveUsers();
        }
    }
}
