package com.yuankai.aispringboot.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadDTO {
    // 1. 文件本体（前端传的 multipart/file）
    // 注意：MultipartFile 不能用 @Max，可以用 @NotNull 校验它是否有文件
    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    // 2. 业务类型（USER_AVATAR, ARTICLE）
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    // 3. 业务对象ID（用户ID，文章ID）
    private String businessId;

    // 4. 业务字段名（avatar, cover）
    private String businessField;
}
