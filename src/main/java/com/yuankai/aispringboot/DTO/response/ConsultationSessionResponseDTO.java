package com.yuankai.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationSessionResponseDTO {
    // 会话ID
    private Long id;

    // 用户ID
    private Long userId;

    // 会话标题
    private String sessionTitle;

    // 开始时间
    private LocalDateTime startedAt;

    // 最后一次情绪分析结果(JSON格式)
    private String lastEmotionAnalysis;

    // 最后一次情绪分析更新时间
    private LocalDateTime lastEmotionUpdatedAt;

    // 消息数量
    private Integer messageCount;
}
