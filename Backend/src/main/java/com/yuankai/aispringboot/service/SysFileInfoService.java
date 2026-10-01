package com.yuankai.aispringboot.service;

import com.yuankai.aispringboot.DTO.FileUploadDTO;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.entity.SysFileInfo;
import com.yuankai.aispringboot.mapper.SysFileInfoMapper;
import com.yuankai.aispringboot.util.ValidateMagicNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
public class SysFileInfoService {
    @Autowired
    private SysFileInfoMapper sysFileInfoMapper;

    @Value("${file.upload-path}")
    private String baseLocalPath;

    // 定义前端访问的基础路径
    private static final String BASE_URL_PATH = "/files/bussiness/";

    // 文件扩展名白名单（防止上传 .exe/.jsp/.html 等危险文件）
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf", "doc", "docx", "txt");

    public String upload(FileUploadDTO dto, Long userId) {
        MultipartFile file = dto.getFile();

        // 文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_CONTENT_INVALID.getMsg());
        }

        // 2. 转换业务类型
        String realBusinessType = convertBusinessType(dto.getBusinessType());
        if (realBusinessType == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getMsg() + ": 不支持的业务类型");
        }

        // 3. 获取扩展名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 4. 校验扩展名白名单
        String extLower = ext.replace(".", "").toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extLower)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED.getMsg() + ": " + ext + "，仅允许图片/PDF/文档");
        }

        // 4.1 校验文件头魔数（文件内容与扩展名必须一致，防 .exe 改名 .jpg 绕过白名单）
        try {
            ValidateMagicNumber.validateMagicNumber(file, extLower);
        } catch (IOException e) {
            log.error("读取文件头失败: {}", originalFilename, e);
            throw new BusinessException(ResultCode.FILE_CONTENT_INVALID.getMsg());
        }

        String timestampName = System.currentTimeMillis() + ext;

        // 5. 拼接最终的本地物理路径和URL路径
        String relativePath = realBusinessType + "/" + timestampName;
        // baseLocalPath 末尾可能没有分隔符，做归一化，防止拼成 "uploaduser_avatar/..."
        String baseDir = baseLocalPath.endsWith("/") || baseLocalPath.endsWith("\\")
                ? baseLocalPath : baseLocalPath + File.separator;
        String localFilePath = baseDir + relativePath;
        String urlPath = BASE_URL_PATH + relativePath;

        // 6. 保存文件到本地
        try {
            File dest = new File(localFilePath);
            // 如果 user_avatar 目录不存在，先创建
            if (!dest.getParentFile().exists()) {
                boolean created = dest.getParentFile().mkdirs();
                if (!created) {
                    log.error("创建目录失败: {}", dest.getParentFile().getAbsolutePath());
                }
            }
            // 把文件写入磁盘
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件保存失败, path={}", localFilePath, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED.getMsg());
        }

        // 7. 构建实体并保存到数据库
        SysFileInfo sysFileInfo = SysFileInfo.builder()
                .originalName(originalFilename)
                .filePath(localFilePath)
                .fileSize(file.getSize())
                .fileType(ext.replace(".", "").toUpperCase())
                .businessType(realBusinessType.toUpperCase())
                .businessId(dto.getBusinessId())
                .businessField(dto.getBusinessField())
                .uploadUserId(userId)
                .isTemp(0)
                .status(1)
                .createTime(LocalDateTime.now())
                .expireTime(null)
                .build();

        sysFileInfoMapper.insert(sysFileInfo);

        return urlPath;
    }

    private String convertBusinessType(String businessType) {
        switch (businessType) {
            case "USER_AVATAR":
                return "user_avatar";
            case "ARTICLE":
                return "article";
            default:
                return null;
        }
    }
}
