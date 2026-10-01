package com.yuankai.aispringboot.controller;

import com.yuankai.aispringboot.DTO.command.UserLoginCommandDTO;
import com.yuankai.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.yuankai.aispringboot.DTO.response.UserLoginResponseDTO;
import com.yuankai.aispringboot.annotation.OperationLog;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.service.UserService;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Resource
    private UserService userService;

    // 用户登录
    @OperationLog("用户登录")
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {  //登录引入Valid校验
        log.info("用户登录: {}", commandDTO.getUsername());
        // 调用服务层的登录方法
        UserLoginResponseDTO result = userService.login(commandDTO);

        return Result.success(result);
    }

    // 用户注册
    @OperationLog("用户注册")
    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO) {
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
        return Result.success(result);
    }

    // 获取当前用户
    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
        Long userId = GetUserInfo.getUserId();

        // 调用Service层获取用户详情
        UserLoginResponseDTO.UserDetailResponseDTO userDetail = userService.getUserById(userId);
        log.info("用户查询成功: {}", userId);

        return Result.success(userDetail);
    }

    // 用户退出登录
    @OperationLog("用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = GetUserInfo.getUserId();

        // 调用Service层将 Token 加入黑名单
        userService.logout(userId);
        log.info("用户 {} 已退出登录", userId);

        return Result.success();
    }

}
