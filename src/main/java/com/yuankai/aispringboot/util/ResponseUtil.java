package com.yuankai.aispringboot.util;

import cn.hutool.json.JSONUtil;
import com.yuankai.aispringboot.common.Result;
import com.yuankai.aispringboot.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {
    private static final Logger log = LoggerFactory.getLogger(ResponseUtil.class);

    // 过滤器中的异常响应
    public static void writeError(HttpServletResponse response, ResultCode resultCode) {
        // 根据不同结果码返回不同的响应
        int status = switch (resultCode) {
            case UNAUTHORIZED, ACCESS_UNAUTHORIZED, TOKEN_INVALID,TOKEN_BLOCKED -> HttpStatus.UNAUTHORIZED.value();
            case TOKEN_ACCESS_FORBIDDEN -> HttpStatus.FORBIDDEN.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter write = response.getWriter()){
            String jsonResponse = JSONUtil.toJsonStr(Result.error(resultCode.getCode(), resultCode.getMsg(), null));
            write.print(jsonResponse);
            write.flush();  // 确保将响应内容写入到输出流
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }
}
