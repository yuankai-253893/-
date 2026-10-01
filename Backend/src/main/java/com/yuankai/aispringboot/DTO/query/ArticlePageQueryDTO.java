package com.yuankai.aispringboot.DTO.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ArticlePageQueryDTO {
    // 当前页码
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码最小为1")
    private Integer currentPage;

    // 每页大小
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size;

    // 排序字段（默认按 readCount 排序）
    @Pattern(regexp = "^(readCount|publishAt|createdAt)$", message = "排序字段不合法")
    private String sortField = "readCount";

    // 排序方向（升序 asc 或 降序 desc，默认降序）
    @Pattern(regexp = "^(asc|desc)$", message = "排序方向只能是 asc 或 desc")
    private String sortDirection = "desc";
}
