package com.yuankai.aispringboot.aspect;

import com.yuankai.aispringboot.annotation.OperationLog;
import com.yuankai.aispringboot.mapper.OperationLogMapper;
import com.yuankai.aispringboot.util.GetUserInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 *
 * 拦截所有标注了 {@link OperationLog} 注解的 Controller 方法，统一记录：
 *   - 谁：登录用户（匿名接口如登录/注册，从请求参数中提取 username）
 *   - 何时：created_at（数据库自动写入）
 *   - 调了什么：HTTP 方法 + 请求地址 + 操作描述
 *   - 请求参数：序列化后写入（password 等敏感字段脱敏，超长截断）
 *   - 耗时：方法执行毫秒数
 *   - 结果：成功 1 / 失败 0（异常信息截断记录，并原样抛出不影响业务）
 *
 * 安全性设计：
 *   1. 日志落库全程 try-catch，日志失败绝不影响业务主流程
 *   2. 密码字段脱敏（password / confirmPassword 替换为 ***）
 *   3. MultipartFile 等不可序列化参数直接过滤，不写入日志
 *   4. 参数/异常信息超长截断，防止日志表被大字段撑爆
 */
@Aspect
@Component
// 显式排在 Spring Security 方法安全拦截器之外层：
// 管理端接口改用 @PreAuthorize 后，鉴权失败会在方法调用前抛 AccessDeniedException。
// 若本切面被包在拦截器内层，被拒绝的请求根本走不到这里，操作日志会丢失一条审计记录；
// 排在外层后，未授权的尝试同样会被 catch 并落库，与改造前（判断写在方法体内）行为一致。
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class OperationLogAspect {
    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    /** 复用 Spring 容器中已配置好的 ObjectMapper（Spring Boot 4 使用 Jackson 3，类型为 tools.jackson.databind.ObjectMapper） */
    @Resource
    private ObjectMapper objectMapper;

    private static final int MAX_PARAM_LENGTH = 1000;
    private static final int MAX_ERROR_LENGTH = 500;
    /** 敏感字段脱敏：password / confirmPassword 的值替换为 *** */
    private static final Pattern SENSITIVE_PATTERN =
            Pattern.compile("(\"(password|confirmPassword)\"\\s*:\\s*\")[^\"]*(\")");

    @Resource
    private OperationLogMapper operationLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        int status = 1;
        String errorMsg = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = 0;
            errorMsg = e.getMessage();
            throw e; // 原样抛出，业务异常仍由全局异常处理器处理
        } finally {
            try {
                saveLog(joinPoint, operationLog, start, status, errorMsg);
            } catch (Exception ex) {
                // 日志落库失败不影响业务，只告警
                log.warn("操作日志保存失败: {}", ex.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperationLog operationLog,
                         long start, int status, String errorMsg) {
        // 1. 提取操作人（登录接口等匿名场景拿不到 SecurityContext，返回 null）
        Long userId = null;
        String username = null;
        try {
            var user = GetUserInfo.getCurrentUser();
            userId = user.getUserId();
            username = user.getUsername();
        } catch (Exception ignored) {
            // 公开接口无登录上下文：从请求参数中提取 username（如登录接口）
            username = extractUsernameFromArgs(joinPoint.getArgs());
        }

        // 2. 组装并写入日志（实体与注解同名，实体用全限定名避免 import 冲突）
        com.yuankai.aispringboot.entity.OperationLog entity = new com.yuankai.aispringboot.entity.OperationLog();
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setOperation(operationLog.value());
        entity.setMethod(requestMethod());
        entity.setRequestUrl(requestUrl());
        entity.setParams(serializeParams(joinPoint.getArgs()));
        entity.setCostTime(System.currentTimeMillis() - start);
        entity.setStatus(status);
        entity.setErrorMsg(truncate(errorMsg, MAX_ERROR_LENGTH));
        operationLogMapper.insert(entity);
    }

    // ---------------- 工具方法 ----------------

    /** 从请求参数对象中尝试提取 username（用于登录/注册等匿名接口） */
    private String extractUsernameFromArgs(Object[] args) {
        for (Object arg : args) {
            try {
                var getter = arg.getClass().getMethod("getUsername");
                Object value = getter.invoke(arg);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception ignored) {
                // 没有 getUsername 方法的参数直接跳过
            }
        }
        return null;
    }

    /** 序列化请求参数：过滤不可序列化类型、脱敏、截断 */
    private String serializeParams(Object[] args) {
        List<Object> serializable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) continue;
            // 过滤框架/文件类型，这些不进入日志
            if (arg instanceof MultipartFile
                    || arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse) {
                continue;
            }
            serializable.add(arg);
        }
        try {
            String json = objectMapper.writeValueAsString(serializable);
            json = SENSITIVE_PATTERN.matcher(json).replaceAll("$1***$3");
            return truncate(json, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    /** 当前请求的 HTTP 方法 */
    private String requestMethod() {
        return currentRequest().map(HttpServletRequest::getMethod).orElse("UNKNOWN");
    }

    /** 当前请求的完整地址 */
    private String requestUrl() {
        return currentRequest()
                .map(req -> req.getRequestURI()
                        + (req.getQueryString() == null ? "" : "?" + req.getQueryString()))
                .orElse("UNKNOWN");
    }

    /** 从请求上下文拿 HttpServletRequest（非 Web 环境返回空） */
    private java.util.Optional<HttpServletRequest> currentRequest() {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttrs) {
            return java.util.Optional.ofNullable(servletAttrs.getRequest());
        }
        return java.util.Optional.empty();
    }

    /** 超长截断 */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
