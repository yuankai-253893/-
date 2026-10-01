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
@TableName("consultation_message")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessage {
    // 消息ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 会话ID
    @TableField("session_id")
    private Long sessionId;

    // 发送者类型 1:用户 2:AI助手
    @TableField("sender_type")
    private Integer senderType;

    // 消息类型 1:文本
    @TableField("message_type")
    private Integer messageType;

    // 消息内容
    private String content;

    // 情绪标签
    @TableField("emotion_tag")
    private String emotionTag;

    // 使用的AI模型
    @TableField("ai_model")
    private String aiModel;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 获取发送者类型描述
     */
    public String getSenderTypeDesc() {
        if (senderType == null) {
            return "未知";
        }
        return switch (senderType) {
            case 1 -> "用户";
            case 2 -> "AI助手";
            default -> "未知";
        };
    }

    /**
     * 获取消息类型描述
     */
    public String getMessageTypeDesc() {
        if (messageType == null) {
            return "未知";
        }
        return switch (messageType) {
            case 1 -> "文本";
            default -> "未知";
        };
    }
}
