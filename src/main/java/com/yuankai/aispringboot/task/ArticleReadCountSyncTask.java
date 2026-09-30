package com.yuankai.aispringboot.task;

import com.yuankai.aispringboot.service.KnowledgeCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文章阅读量定时刷库任务：
 * 阅读量自增走 Redis INCR，本任务定期把增量刷回 MySQL，保证最终一致。
 * 为什么不用固定间隔：fixedDelay 表示上次执行完成后隔 60 秒再执行，
 * 不会出现任务重叠；刷库本身很快，单线程足够。
 */
@Slf4j
@Component
public class ArticleReadCountSyncTask {

    @Autowired
    private KnowledgeCategoryService knowledgeCategoryService;

    // 每 60 秒执行一次：把 Redis 中的阅读量增量同步到 MySQL 并清空增量
    @Scheduled(fixedDelay = 60_000)
    public void syncArticleReadCounts() {
        try {
            knowledgeCategoryService.syncArticleReadCounts();
        } catch (Exception e) {
            // 定时任务异常不能影响主流程，记录日志后下次重试
            log.error("阅读量定时刷库任务执行失败", e);
        }
    }
}
