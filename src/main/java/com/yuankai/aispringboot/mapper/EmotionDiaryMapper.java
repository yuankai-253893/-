package com.yuankai.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuankai.aispringboot.entity.EmotionDiary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmotionDiaryMapper extends BaseMapper<EmotionDiary> {
    @Select("""
        SELECT diary_date      AS diaryDate,
               AVG(mood_score) AS avgMoodScore,
               COUNT(*)        AS diaryCount
        FROM emotion_diary
        WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
        GROUP BY diary_date
        ORDER BY diary_date
        """)
    List<Map<String, Object>> selectLast7DaysMoodStats();

    @Select("""
        SELECT COUNT(DISTINCT uid) FROM (
        SELECT user_id AS uid FROM emotion_diary
        WHERE DATE(created_at) = CURDATE()
        UNION                                   
        SELECT user_id AS uid FROM consultation_session
        WHERE DATE(started_at) = CURDATE()
       ) t
    """)
    Long selectTodayActiveUsers();
}
