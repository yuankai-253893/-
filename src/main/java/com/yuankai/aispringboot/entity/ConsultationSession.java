package com.yuankai.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("consultation_session")
@Builder        // 实体转成数据
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSession {
    // 会话ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 会话标题
    @TableField("session_title")
    private String sessionTitle;

    // 开始时间
    @TableField("started_at")
    private LocalDateTime startedAt;

    // 最后一次情绪分析结果(JSON格式)
    @TableField("last_emotion_analysis")
    private String lastEmotionAnalysis;

    // 最后一次情绪分析更新时间
    @TableField("last_emotion_updated_at")
    private LocalDateTime lastEmotionUpdatedAt;
}
