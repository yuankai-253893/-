package com.yuankai.aispringboot.common;

import com.yuankai.aispringboot.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

//拦截异常，返回统一结果
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handlerException(MethodArgumentNotValidException e) {
        // 异常数据的处理
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), message);
    }

    //处理GET请求参数绑定异常（@Valid + query参数）
    @ExceptionHandler(BindException.class)
    public Result<String> handlerBindException(BindException e) {
        // 异常数据的处理
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), message);
    }

    //处理请求体JSON解析失败
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handlerHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), "请求体JSON格式错误");
    }

    //处理路径/请求参数类型转换失败
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handlerMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), "参数类型不合法");
    }

    //处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handlerBusinessException(BusinessException e) {
        //如果异常携带有额外数据
        if (e.getData() != null) {
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        return Result.error(e.getCode(), e.getMessage(), null);
    }

    /**
     * 处理 @PreAuthorize 方法级鉴权拒绝。
     *
     * 为什么必须单独处理：@PreAuthorize 是 AOP 方法级拦截，拒绝时抛出的
     * AuthorizationDeniedException 不会经过 Spring Security 的 ExceptionTranslationFilter
     * （那个过滤器只兜 URL 级授权），会直接冒到 ControllerAdvice。
     * 若不加本处理器，会被下方 @ExceptionHandler(Exception.class) 兜底成"系统错误 500"，
     * 前端会把权限不足误判为服务器故障。
     *
     * 统一返回 ACCESS_UNAUTHORIZED（A0301 访问未授权），与改造前手写判断的返回体一致。
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public Result<?> handlerAuthorizationDenied(AuthorizationDeniedException e) {
        log.warn("方法级鉴权拒绝: {}", e.getMessage());
        return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);
    }

    //兼容旧版/其他来源的 AccessDeniedException（如 Spring Security 5 风格或自定义抛出的）
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handlerAccessDenied(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);
    }

    //处理文件上传大小超限（Spring multipart 在进入 Controller 前抛出）
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handlerMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return Result.error(ResultCode.FILE_SIZE_EXCEEDED.getCode(), ResultCode.FILE_SIZE_EXCEEDED.getMsg(), null);
    }

    //兜底：捕获所有未处理的异常
    @ExceptionHandler(Exception.class)
    public Result<?> handlerException(Exception e) {
        log.error("未捕获的异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMsg(), null);
    }
}

