package com.yuankai.aispringboot.DTO.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmotionDiaryQueryDTO {
    // 当前页码
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码最小为1")
    private Integer current;

    // 每页大小
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size;

    // 用户ID
    private Long userId;

    // 最小情绪分数
    @Min(value = 1, message = "最小情绪分数最小为1")
    private Integer minMoodScore;

    // 最大情绪分数
    @Max(value = 10, message = "最大情绪分数最大为10")
    private Integer maxMoodScore;

    // 主要情绪
    private String dominantEmotion;
}
