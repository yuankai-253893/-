package com.yuankai.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class CategoryResponseDTO {
    // 分类ID
    private Long id;

    // 父分类ID
    private Long parentId;

    // 分类名称
    private String categoryName;

    // 分类代码
    private String categoryCode;

    // 分类描述
    private String description;

    // 排序
    private Integer sortOrder;

    // 状态 0:禁用 1:正常
    private Integer status;

    // 创建时间
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 子分类列表（树形结构用）
    private List<CategoryResponseDTO> children;


}
