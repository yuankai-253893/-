package com.yuankai.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataAnalyticsResponseDTO {
    // 用户总数
    private Long userTotal;

    // 今日活跃
    private Long todayActive;

    // 情绪日记总数
    private Long emotionDiaryTotal;

    // 日记情绪均值
    private Double avgMoodScore;

    // 咨询会话数
    private Long totalSessions;

    // 消息数
    private Long messageTotal;

    // 近 7 日情绪趋势
    private List<EmotionTrend> emotionTrend;

    @Data
    @Builder
    public static class EmotionTrend {
        // 日记日期
        private String date;

        // 平均情绪分数
        private Double avgMoodScore;

        // 日记数量
        private Long diaryCount;
    }

}
