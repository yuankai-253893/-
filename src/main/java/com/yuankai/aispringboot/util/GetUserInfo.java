package com.yuankai.aispringboot.util;

import com.auth0.jwt.interfaces.DecodedJWT;

// 从token中获取用户信息
public class GetUserInfo {
    // 获取用户ID
    public static Long getUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    // 获取用户名（token中的claim名为username）
    public static String getUserName() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("username").asString();
    }

    // 获取用户角色类型（token中的claim名为roleType，与JwtTokenUtil.generateToken保持一致）
    public static Integer getUserType() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("roleType").asInt();
    }
}
