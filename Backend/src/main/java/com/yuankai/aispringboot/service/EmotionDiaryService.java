package com.yuankai.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuankai.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import com.yuankai.aispringboot.DTO.query.EmotionDiaryQueryDTO;
import com.yuankai.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.entity.EmotionDiary;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.mapper.EmotionDiaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class EmotionDiaryService {
    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private ActiveUserRecordService activeUserRecordService;

    public EmotionDiaryResponseDTO createOrUpdateEmotionDiary(Long userId, EmotionDiaryCreateDTO dto) {
        if (dto.getDiaryDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "日记日期不能晚于今天");
        }

        // 命中 user_date_unique(user_id, diary_date) 唯一索引冲突时 MySQL 自动转 UPDATE，
        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                .diaryDate(dto.getDiaryDate())
                .moodScore(dto.getMoodScore())
                .dominantEmotion(dto.getDominantEmotion())
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .build();
        emotionDiaryMapper.upsertEmotionDiary(diary);

        // 活跃埋点：写/更新情绪日记视为一次今日活跃（Redis HyperLogLog 去重计数）
        activeUserRecordService.record(userId);

        // 回查最新记录返回（拿到 id、AI 分析字段等）
        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, dto.getDiaryDate());
        EmotionDiary saved = emotionDiaryMapper.selectOne(queryWrapper);
        return convertToResponseDTO(saved);
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
