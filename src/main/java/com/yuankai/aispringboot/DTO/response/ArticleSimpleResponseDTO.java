package com.yuankai.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ArticleSimpleResponseDTO {
    private String id;

    private String title;

    private Long categoryId;

    private String summary;

    private Integer readCount;

    private Integer status;

    private String tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishAt;

    // 作者名称
    private String authorName;
}
