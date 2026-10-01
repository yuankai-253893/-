package com.yuankai.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
public class EmotionDiaryResponseDTO {
    // 日记ID
    private Long id;

    // 用户ID
    private Long userId;

    // 日记日期
    private LocalDate diaryDate;

    // 情绪评分(1-10)
    private Integer moodScore;

    // 主要情绪
    private String dominantEmotion;

    // 情绪触发因素
    private String emotionTriggers;

    // 日记内容
    private String diaryContent;

    // 睡眠质量(1-5)
    private Integer sleepQuality;

    // 压力水平(1-5)
    private Integer stressLevel;

    // AI情绪分析结果(JSON格式)
    private String aiEmotionAnalysis;

    // AI分析更新时间
    private LocalDateTime aiAnalysisUpdatedAt;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
