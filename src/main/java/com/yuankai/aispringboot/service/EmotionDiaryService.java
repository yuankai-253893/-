package com.yuankai.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import com.yuankai.aispringboot.DTO.query.EmotionDiaryQueryDTO;
import com.yuankai.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.entity.EmotionDiary;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.mapper.EmotionDiaryMapper;
import com.yuankai.aispringboot.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
public class EmotionDiaryService {
    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private UserMapper userMapper;

    public EmotionDiaryResponseDTO createOrUpdateEmotionDiary(Long userId, EmotionDiaryCreateDTO dto) {
        // 根据 userId + diaryDate 查询是否存在（表上有 user_date_unique 唯一索引，每天每用户只有一条）
        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, dto.getDiaryDate());
        EmotionDiary exist = emotionDiaryMapper.selectOne(queryWrapper);

        if (exist == null) {
            // 插入，创建/更新时间赋值当前时间
            EmotionDiary diary = EmotionDiary.builder()
                    .userId(userId)
                    .diaryDate(dto.getDiaryDate())
                    .moodScore(dto.getMoodScore())
                    .dominantEmotion(dto.getDominantEmotion())
                    .emotionTriggers(dto.getEmotionTriggers())
                    .diaryContent(dto.getDiaryContent())
                    .sleepQuality(dto.getSleepQuality())
                    .stressLevel(dto.getStressLevel())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            emotionDiaryMapper.insert(diary);
            return convertToResponseDTO(diary);
        }

        // 更新，注意updated_at
        LambdaUpdateWrapper<EmotionDiary> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EmotionDiary::getId, exist.getId())
                .set(EmotionDiary::getMoodScore, dto.getMoodScore())
                .set(EmotionDiary::getDominantEmotion, dto.getDominantEmotion())
                .set(EmotionDiary::getEmotionTriggers, dto.getEmotionTriggers())
                .set(EmotionDiary::getDiaryContent, dto.getDiaryContent())
                .set(EmotionDiary::getSleepQuality, dto.getSleepQuality())
                .set(EmotionDiary::getStressLevel, dto.getStressLevel())
                .set(EmotionDiary::getUpdatedAt, LocalDateTime.now());
        emotionDiaryMapper.update(null, updateWrapper);
        EmotionDiary updated = emotionDiaryMapper.selectById(exist.getId());
        return convertToResponseDTO(updated);
    }

    public Page<EmotionDiaryResponseDTO> getEmotionDiaryByPage(EmotionDiaryQueryDTO queryDTO) {
        // 构建分页对象
        Page<EmotionDiary> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 构建查询条件
        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getUserId() != null)
            queryWrapper.eq(EmotionDiary::getUserId, queryDTO.getUserId());

        // 如果提供了主要情绪，按主要情绪模糊匹配
        if (StrUtil.isNotBlank(queryDTO.getDominantEmotion())) {
            queryWrapper.like(EmotionDiary::getDominantEmotion, queryDTO.getDominantEmotion());
        }

        if (queryDTO.getMinMoodScore() != null && queryDTO.getMaxMoodScore() != null
                && queryDTO.getMinMoodScore() > queryDTO.getMaxMoodScore()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "最小情绪分数不能大于最大情绪分数");
        }

        // 如果同时提供了最大情绪分数和最小情绪分数，则 筛选情绪分数在 最小情绪分数 和 最大情绪分数 之间的
        if (queryDTO.getMinMoodScore() != null && queryDTO.getMaxMoodScore() != null) {
            queryWrapper.between(EmotionDiary::getMoodScore, queryDTO.getMinMoodScore(), queryDTO.getMaxMoodScore());
        }
        // 如果提供了 最小情绪分数，则筛选 情绪分数分数 比 最小情绪分数 高的
        else if (queryDTO.getMinMoodScore() != null) {
            queryWrapper.ge(EmotionDiary::getMoodScore, queryDTO.getMinMoodScore());
        }
        // 如果提供了 最大情绪分数，则筛选 情绪分数分数 比 最大情绪分数 低的
        else if (queryDTO.getMaxMoodScore() != null) {
            queryWrapper.le(EmotionDiary::getMoodScore, queryDTO.getMaxMoodScore());
        }

        // 按开始时间倒叙排列
        queryWrapper.orderByDesc(EmotionDiary::getCreatedAt);

        Page<EmotionDiary> emotionDiaryPage = emotionDiaryMapper.selectPage(page, queryWrapper);

        // 转换为响应DTO
        Page<EmotionDiaryResponseDTO> responsePage = new Page<>(emotionDiaryPage.getCurrent(), emotionDiaryPage.getSize(), emotionDiaryPage.getTotal());
        responsePage.setRecords(emotionDiaryPage.getRecords().stream().map(this::convertToResponseDTO).toList());


        return responsePage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEmotionDiary(Long id, Long adminId) {
        log.info("管理员{}删除情绪日记，id: {}", adminId, id);
        int deletedRow = emotionDiaryMapper.deleteById(id);
        if (deletedRow == 0)
            throw new BusinessException(ResultCode.EMOTIONDIARY_NOT_FOUND.getCode(), ResultCode.EMOTIONDIARY_NOT_FOUND.getMsg());
    }

    private EmotionDiaryResponseDTO convertToResponseDTO(EmotionDiary diary) {
        return EmotionDiaryResponseDTO.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .diaryDate(diary.getDiaryDate())
                .moodScore(diary.getMoodScore())
                .dominantEmotion(diary.getDominantEmotion())
                .emotionTriggers(diary.getEmotionTriggers())
                .diaryContent(diary.getDiaryContent())
                .sleepQuality(diary.getSleepQuality())
                .stressLevel(diary.getStressLevel())
                .aiEmotionAnalysis(diary.getAiEmotionAnalysis())
                .aiAnalysisUpdatedAt(diary.getAiAnalysisUpdatedAt())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

}
