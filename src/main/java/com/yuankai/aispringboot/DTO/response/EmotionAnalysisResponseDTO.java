package com.yuankai.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmotionAnalysisResponseDTO {
    // 会话ID
    private Long sessionId;

    // 最后一次情绪分析结果(解析后的JSON对象)，尚未生成时为null
    private Object lastEmotionAnalysis;

    // 最后一次情绪分析更新时间
    private LocalDateTime lastEmotionUpdatedAt;
}
