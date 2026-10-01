package com.yuankai.aispringboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleStatusUpdateDTO {
    // 知识文章状态（1-发布，0-下线）
    @NotNull(message = "状态不能为空")
    private Integer status;
}
