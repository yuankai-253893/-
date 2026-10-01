package com.yuankai.aispringboot.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.yuankai.aispringboot.DTO.query.ConsultationSessionQueryDTO;
import com.yuankai.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.yuankai.aispringboot.DTO.response.EmotionAnalysisResponseDTO;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.entity.ConsultationMessage;
import com.yuankai.aispringboot.entity.ConsultationSession;
import com.yuankai.aispringboot.entity.User;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.mapper.ConsultationMessageMapper;
import com.yuankai.aispringboot.mapper.ConsultationSessionMapper;
import com.yuankai.aispringboot.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ConsultationSessionService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConsultationSessionMapper consultationSessionMapper;

    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;

    public ConsultationSession createSession (Long userId, ConsultationSessionCreateDTO createDTO){
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 创建会话记录
            ConsultationSession session = ConsultationSession.builder()
                    .userId(userId)
                    .sessionTitle(createDTO.getSessionTitle())
                    .startedAt(LocalDateTime.now())
                    .build();
            // 如果未提供标题
            if (StrUtil.isBlank(createDTO.getSessionTitle())) {
                // 设置默认标题
                session.setSessionTitle("AI助手-" + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm"));
            }

            // 插入记录
            consultationSessionMapper.insert(session);
            return session;

        }
        return null;
    }

    // 根据会话ID查询会话，用于校验会话归属
    public ConsultationSession getConsultationSessionBySessionId(Long sessionId) {
        return consultationSessionMapper.selectById(sessionId);
    }

    public Page<ConsultationSessionResponseDTO> getSessionsByPage(Long userId, ConsultationSessionQueryDTO queryDTO) {
        // 构建分页对象
        Page<ConsultationSession> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize());

        // 构建查询条件
        LambdaQueryWrapper<ConsultationSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationSession::getUserId, userId);

        // 如果提供了情绪标签，按最后情绪分析结果模糊匹配
        if (StrUtil.isNotBlank(queryDTO.getEmotionTag())) {
            queryWrapper.like(ConsultationSession::getLastEmotionAnalysis, queryDTO.getEmotionTag());
        }

        // 按开始时间倒序排列
        queryWrapper.orderByDesc(ConsultationSession::getStartedAt);

        // 执行分页查询
        Page<ConsultationSession> sessionPage = consultationSessionMapper.selectPage(page, queryWrapper);

        // 转换为响应DTO
        Page<ConsultationSessionResponseDTO> responsePage = new Page<>(sessionPage.getCurrent(), sessionPage.getSize(), sessionPage.getTotal());
        responsePage.setRecords(sessionPage.getRecords().stream().map(this::convertToResponseDTO).toList());

        return responsePage;
    }

    private ConsultationSessionResponseDTO convertToResponseDTO(ConsultationSession session) {
        ConsultationSessionResponseDTO responseDTO = new ConsultationSessionResponseDTO();
        responseDTO.setId(session.getId());
        responseDTO.setUserId(session.getUserId());
        responseDTO.setSessionTitle(session.getSessionTitle());
        responseDTO.setStartedAt(session.getStartedAt());
        responseDTO.setLastEmotionAnalysis(session.getLastEmotionAnalysis());
        responseDTO.setLastEmotionUpdatedAt(session.getLastEmotionUpdatedAt());

        // 查询该会话的消息数量
        LambdaQueryWrapper<ConsultationMessage> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(ConsultationMessage::getSessionId, session.getId());
        Long messageCount = consultationMessageMapper.selectCount(countWrapper);
        responseDTO.setMessageCount(messageCount.intValue());

        return responseDTO;
    }

    // 删除会话，并级联删除该会话下的所有消息
    @Transactional(rollbackFor = Exception.class)   // 失败后回滚
    public void deleteSession(Long sessionId, Long userId) {
        // 先级联删除会话下的消息，避免残留孤儿数据
        LambdaQueryWrapper<ConsultationMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(ConsultationMessage::getSessionId, sessionId);
        consultationMessageMapper.delete(messageWrapper);

        // 再删除会话本身，通过删除行数判断会话是否存在
        int deletedRows = consultationSessionMapper.deleteById(sessionId);
        if (deletedRows == 0) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND.getCode(), ResultCode.SESSION_NOT_FOUND.getMsg());
        }

        log.info("用户{}删除会话，id: {}", userId, sessionId);

    }

    // 读取会话的情绪分析结果
    public EmotionAnalysisResponseDTO getEmotionAnalysisBySessionId(Long sessionId) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND.getCode(), ResultCode.SESSION_NOT_FOUND.getMsg());
        }

        EmotionAnalysisResponseDTO responseDTO = new EmotionAnalysisResponseDTO();
        responseDTO.setSessionId(session.getId());
        responseDTO.setLastEmotionUpdatedAt(session.getLastEmotionUpdatedAt());

        // 情绪分析结果以JSON字符串存库，解析后返回
        if (StrUtil.isNotBlank(session.getLastEmotionAnalysis())) {
            responseDTO.setLastEmotionAnalysis(JSONUtil.parse(session.getLastEmotionAnalysis()));
        }
        return responseDTO;
    }
}
