package com.yuankai.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuankai.aispringboot.DTO.response.DataAnalyticsResponseDTO;
import com.yuankai.aispringboot.entity.ConsultationMessage;
import com.yuankai.aispringboot.entity.ConsultationSession;
import com.yuankai.aispringboot.entity.EmotionDiary;
import com.yuankai.aispringboot.entity.User;
import com.yuankai.aispringboot.mapper.ConsultationMessageMapper;
import com.yuankai.aispringboot.mapper.ConsultationSessionMapper;
import com.yuankai.aispringboot.mapper.EmotionDiaryMapper;
import com.yuankai.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataAnalyticsService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private ConsultationSessionMapper consultationSessionMapper;

    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;

    @Autowired
    private ActiveUserRecordService activeUserRecordService;

    public DataAnalyticsResponseDTO getDataAnalytics() {
        // 各表总数
        Long userTotal = userMapper.selectCount(null);
        Long emotionDiaryTotal = emotionDiaryMapper.selectCount(null);
        Long totalSessions = consultationSessionMapper.selectCount(null);
        Long messageTotal = consultationMessageMapper.selectCount(null);

        // 日记情绪均值
        QueryWrapper<EmotionDiary> avgWrapper = new QueryWrapper<>();
        avgWrapper.select("AVG(mood_score) AS averageMoodScore");
        Map<String, Object> avgResult = emotionDiaryMapper.selectMaps(avgWrapper).get(0);
        Double avgMoodScore = toDouble(avgResult.get("averageMoodScore"));

        // 今日活跃用户（Redis HyperLogLog 去重计数，Redis 故障自动回退 SQL）
        Long todayActive = activeUserRecordService.countToday();

        // 近 7 日情绪趋势：Map -> EmotionTrend 列表
        List<Map<String, Object>> stats = emotionDiaryMapper.selectLast7DaysMoodStats();
        List<DataAnalyticsResponseDTO.EmotionTrend> trend = stats.stream()
                .map(m -> DataAnalyticsResponseDTO.EmotionTrend.builder()
                        .date(m.get("diaryDate") == null ? null : m.get("diaryDate").toString())
                        .avgMoodScore(toDouble(m.get("avgMoodScore")))
                        .diaryCount(toLong(m.get("diaryCount")))
                        .build())
                .collect(Collectors.toList());

        return DataAnalyticsResponseDTO.builder()
                .userTotal(userTotal)
                .todayActive(todayActive)
                .emotionDiaryTotal(emotionDiaryTotal)
                .avgMoodScore(avgMoodScore)
                .totalSessions(totalSessions)
                .messageTotal(messageTotal)
                .emotionTrend(trend)
                .build();
    }

    // MySQL 的 AVG 返回 BigDecimal、COUNT 返回 Long，统一用 Number 父类安全转 Double
    private Double toDouble(Object v) {
        return v == null ? null : ((Number) v).doubleValue();
    }

    private Long toLong(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }
}
