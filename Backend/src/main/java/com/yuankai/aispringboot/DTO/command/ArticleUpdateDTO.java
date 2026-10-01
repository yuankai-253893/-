package com.yuankai.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleUpdateDTO {
    @NotBlank(message = "文章标题不能为空")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String coverImage;

    @NotNull(message = "文章分类ID不能为空")
    private Long categoryId;

    private String summary;

    private String tags;

    @NotBlank(message = "文章id不能为空")
    private String id;
}
