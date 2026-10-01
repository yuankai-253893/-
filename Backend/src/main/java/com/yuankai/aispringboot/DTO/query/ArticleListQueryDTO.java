package com.yuankai.aispringboot.DTO.query;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class ArticleListQueryDTO {
    // 文章标题（可选，模糊搜索）
    @Size(max = 200, message = "文章标题不能超过200个字符")
    private String title;

    // 文章分类ID（可选，按分类筛选）
    private Long categoryId;

    // 文章状态：1表示已发布
    private Integer status;

    // 作者名称
    private String authorName;

    // 当前页码
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码最小为1")
    private Integer currentPage;

    // 每页大小
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size;
}
