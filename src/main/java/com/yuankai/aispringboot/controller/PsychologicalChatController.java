package com.yuankai.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.AiService.PsychologicalSupportService;
import com.yuankai.aispringboot.AiService.StructOutPut;
import com.yuankai.aispringboot.annotation.OperationLog;
import com.yuankai.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.yuankai.aispringboot.DTO.command.ConsultationStreamDTO;
import com.yuankai.aispringboot.DTO.query.ConsultationSessionQueryDTO;
import com.yuankai.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.yuankai.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.yuankai.aispringboot.DTO.response.EmotionAnalysisResponseDTO;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.entity.ConsultationSession;
import com.yuankai.aispringboot.enumclass.UserType;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.service.ConsultationMessageService;
import com.yuankai.aispringboot.service.ConsultationSessionService;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChatController {

    // SSE 分片合并参数：攒够 8 片、或距上次发送超过 30ms 就立即发出一批
    private static final int BUFFER_MAX_SIZE = 8;
    private static final Duration BUFFER_FLUSH_INTERVAL = Duration.ofMillis(30);

    @Autowired
    private PsychologicalSupportService psychologicalSupportService;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    // 开始会话
    @OperationLog("开启咨询会话")
    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        Long userId = GetUserInfo.getUserId();

        StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.success(session);
    }

    // 流式对话
    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        Long userId = GetUserInfo.getUserId();
        Integer roleType = GetUserInfo.getUserType();

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(),"用户未登录")))
                    .build());
        }

        // 普通用户：校验会话归属，防止往他人会话写入消息 / 借用他人会话上下文（水平越权）
        if (UserType.USER.getCode().equals(roleType)) {
            Long dbSessionId = PsychologicalSupportService.extractSessionId(streamDTO.getSessionId());
            ConsultationSession session = consultationSessionService.getConsultationSessionBySessionId(dbSessionId);
            if (session == null || !userId.equals(session.getUserId())) {
                return Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(JSONUtil.toJsonStr(Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(),"无权访问该会话")))
                        .build());
            }
        }
        // 管理员：不校验归属，可向任意会话对话

        // 开始流式对话
        return psychologicalSupportService.streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage())
                // 分片合并：模型吐 token 很快时，把短时间窗内的碎片攒成一批再发一次 SSE，
                // 既减少帧数和浏览器渲染压力，又不像 delayElements 那样给每个元素累加延迟
                .bufferTimeout(BUFFER_MAX_SIZE, BUFFER_FLUSH_INTERVAL)
                .filter(fragments -> !fragments.isEmpty())
                .map(fragments -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(JSONUtil.toJsonStr(Result.success(Map.of("content", String.join("", fragments), "type", "normal"))))
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()
                ));
    }

    // 分页查询会话
    @GetMapping("/sessions")
    public Result<Page<ConsultationSessionResponseDTO>> getSessions(@Valid ConsultationSessionQueryDTO queryDTO) {
        Long userId = GetUserInfo.getUserId();

        Page<ConsultationSessionResponseDTO> sessionPage = consultationSessionService.getSessionsByPage(userId, queryDTO);
        return Result.success(sessionPage);
    }

    // 查询会话消息
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> getMessages(@PathVariable Long sessionId) {
        Long userId = GetUserInfo.getUserId();
        Integer roleType = GetUserInfo.getUserType();

        // 普通用户：校验会话归属，仅能查看自己的会话消息
        if (UserType.USER.getCode().equals(roleType)) {
            ConsultationSession session = consultationSessionService.getConsultationSessionBySessionId(sessionId);
            if (session == null || !userId.equals(session.getUserId())) {
                throw new BusinessException(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg());
            }
        }
        // 管理员：不做归属校验，可查看所有会话消息
        
        List<ConsultationMessageResponseDTO> messages = consultationMessageService.getMessagesBySessionId(sessionId);
        return Result.success(messages);
    }

    // 删除会话
    @DeleteMapping("/sessions/{sessionId}")
    public Result<?> deleteSession(@PathVariable Long sessionId) {
        Long userId = GetUserInfo.getUserId();
        Integer roleType = GetUserInfo.getUserType();

        // 普通用户：校验会话归属，仅能删除自己的会话
        if (UserType.USER.getCode().equals(roleType)) {
            ConsultationSession session = consultationSessionService.getConsultationSessionBySessionId(sessionId);
            if (session == null || !userId.equals(session.getUserId())) {
                throw new BusinessException(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg());
            }
        }
        // 管理员：不做归属校验，可删除所有会话

        consultationSessionService.deleteSession(sessionId, userId);
        return Result.success();
    }

    // 获取会话情绪分析结果
    @GetMapping("/session/{sessionId}/emotion")
    public Result<EmotionAnalysisResponseDTO> getEmotionAnalysis(@PathVariable Long sessionId) {
        Long userId = GetUserInfo.getUserId();
        Integer roleType = GetUserInfo.getUserType();

        // 普通用户：校验会话归属，仅能查看自己会话的情绪分析
        if (UserType.USER.getCode().equals(roleType)) {
            ConsultationSession session = consultationSessionService.getConsultationSessionBySessionId(sessionId);
            if (session == null || !userId.equals(session.getUserId())) {
                throw new BusinessException(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg());
            }
        }
        // 管理员：不做归属校验，可查看任意会话的情绪分析

        EmotionAnalysisResponseDTO emotionAnalysis = consultationSessionService.getEmotionAnalysisBySessionId(sessionId);
        return Result.success(emotionAnalysis);
    }

}
