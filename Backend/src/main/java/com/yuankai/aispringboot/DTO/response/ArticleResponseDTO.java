package com.yuankai.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ArticleResponseDTO {
    // 文章ID（UUID）
    private String id;

    // 文章分类ID
    private Long categoryId;

    // 文章标题
    private String title;

    // 文章摘要
    private String summary;

    // 文章内容
    private String content;

    // 封面图片
    private String cover;

    // 标签
    private String tags;

    // 作者ID
    private Long authorId;

    // 阅读次数
    private Integer readCount;

    // 文章状态
    private Integer status;

    // 发布时间
    private LocalDateTime publishAt;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
