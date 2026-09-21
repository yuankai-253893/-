package com.yuankai.aispringboot.DTO.command;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmotionDiaryCreateDTO {
    // 日记日期
    @NotNull(message = "日记日期不能为空")
    private LocalDate diaryDate;

    // 情绪评分(1-10)
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分最低为1分")
    @Max(value = 10, message = "情绪评分最高为10分")
    private Integer moodScore;

    // 主要情绪
    @Size(max = 50, message = "主要情绪长度不能超过50个字符")
    private String dominantEmotion;

    // 情绪触发因素
    private String emotionTriggers;

    // 日记内容
    private String diaryContent;

    // 睡眠质量(1-5)
    @Min(value = 1, message = "睡眠质量最低为1分")
    @Max(value = 5, message = "睡眠质量最高为5分")
    private Integer sleepQuality;

    // 压力水平(1-5)
    @Min(value = 1, message = "压力水平最低为1分")
    @Max(value = 5, message = "压力水平最高为5分")
    private Integer stressLevel;

}
