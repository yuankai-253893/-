package com.yuankai.aispringboot.AiService;


import com.yuankai.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.yuankai.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.yuankai.aispringboot.entity.ConsultationSession;
import com.yuankai.aispringboot.service.ActiveUserRecordService;
import com.yuankai.aispringboot.service.ConsultationMessageService;
import com.yuankai.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class PsychologicalSupportService {
    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ActiveUserRecordService activeUserRecordService;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 创建数据库的会话记录，向数据库插入一条会话记录
        ConsultationSession ConsultationSession = consultationSessionService.createSession(userId, createDTO);

        // 活跃埋点：开启咨询会话视为一次今日活跃（Redis HyperLogLog 去重计数）
        activeUserRecordService.record(userId);

        // 将初始用户消息保存到message表
        consultationMessageService.saveUserMessage(ConsultationSession.getId(), createDTO.getInitialMessage(), null);

        // 创建会话信息
        String sessionId = "session_" + ConsultationSession.getId();
        return  new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L , // 会话有效期1天
                1,
                "ACTIVE"
        );
    }

    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage) {
        // 创建响应流
        return Flux.create(sink -> {
            Long dbSessionId = extractSessionId(sessionId);

            if (dbSessionId == null) {
                sink.error(new RuntimeException("会话ID格式错误"));
                return;
            }

            // 是否为初始消息
            boolean isInitialMessage = false;
            // 检查是否为初始消息，避免重复保存
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }
            }
            if (!isInitialMessage) {
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
            }

            // 进行流式对话
            // 生成对话记忆管理
            String conversationId = "conversation_" + sessionId;
            // 构建系统提示词
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationId, userMessages);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
            ));

            // 用于存储AI完成的响应
            StringBuilder aiResponse = new StringBuilder();

            // 使用ChatClient发送消息到Open Ai
            chatClient.prompt(prompt)
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()                           // 流式请求
                    .content()                          // 把流里的内容提取成 `Flux<String>`（纯文本片段）
                    .doOnNext(Fragment -> {
                        aiResponse.append(Fragment);    // 把ai的回复攒起来
                        sink.next(Fragment);            // 立刻转发给前端
                    })
                    .doOnComplete(() -> {
                        String completeRes = aiResponse.toString();     // 拿到完整回答
                        // 保存AI的响应消息到数据库
                        consultationMessageService.saveAiMessage(dbSessionId, completeRes, "openai");
                        // 添加AI响应消息到会话记忆
                        List<Message> aiMessages = new ArrayList<>();
                        aiMessages.add(new AssistantMessage(completeRes));
                        chatMemory.add(conversationId, aiMessages);

                        sink.complete();
                    })
                    .doOnError(error -> {
                        sink.error(error);
                    })
                    .subscribe();  // 订阅并启动流
        });
    }

    // 获取参数中的sessionId
    public static Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            String idstr = sessionId.substring("session_".length());
            return Long.parseLong(idstr);
        }
        return null;
    }
}
