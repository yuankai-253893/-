package com.yuankai.aispringboot.controller;

import com.yuankai.aispringboot.DTO.FileUploadDTO;
import com.yuankai.aispringboot.annotation.OperationLog;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.service.SysFileInfoService;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/file/upload")
public class FileController {
    @Autowired
    private SysFileInfoService sysFileInfoService;

    @OperationLog("文件上传")
    @PostMapping
    public Result<String> uploadFile(@Valid FileUploadDTO fileUploadDTO) {
        Long userId = GetUserInfo.getUserId();
        // 调用 Service 层逻辑，返回文件访问路径
        String url = sysFileInfoService.upload(fileUploadDTO, userId);

        log.info("用户{}上传文件成功，文件访问路径{}", userId, url);
        return Result.success(url);
    }
}
