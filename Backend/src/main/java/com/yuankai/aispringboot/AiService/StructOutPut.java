package com.yuankai.aispringboot.AiService;

// 定义输出结构
public class StructOutPut {
    public record StreamChatSession(
            String sessionId,
            Long userId,
            String initialMessage,
            Long startTime,
            Long expireTime,
            Integer messageCount,
            String status
    ) { }
}
