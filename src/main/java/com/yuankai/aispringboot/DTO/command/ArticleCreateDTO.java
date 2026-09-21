package com.yuankai.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleCreateDTO {
    // 文章标题
    @NotBlank(message = "文章标题不能为空")
    private String title;

    // 文章内容
    @NotBlank(message = "文章内容不能为空")
    private String content;

    // 封面图片
    private String coverImage;

    // 文章分类ID
    @NotNull(message = "文章分类ID不能为空")
    private Long categoryId;

    // 文章摘要
    private String summary;

    // 标签
    private String tags;

    // 文章ID
    private String id;
}
