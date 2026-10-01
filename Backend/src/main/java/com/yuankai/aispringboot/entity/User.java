package com.yuankai.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuankai.aispringboot.enumclass.UserStatus;
import com.yuankai.aispringboot.enumclass.UserType;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

//用户实体类
@Builder
@Data
@TableName("user")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // 用户ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //用户名
    private String username;

    // 邮箱
    private String email;

    // 手机号
    private String phone;

    // 密码
    private String password;

    // 昵称
    private String nickname;

    // 头像
    private String avatar;

    // 性别
    private Integer gender;

    // 生日
    private LocalDate birthday;

    // 用户类型 1:普通用户 2:管理员
    @TableField("user_type")
    @Builder.Default
    private Integer userType = 1;

    // 状态 0:禁用 1:正常
    @Builder.Default
    private Integer status = 1;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

//    /**
//     * 是否为普通用户
//     */
//    public boolean isUser() {
//        return UserType.USER.getCode().equals(this.userType);
//    }
//
    /**
     * 是否为正常状态
     */
    public boolean isActive() {
        return UserStatus.NORMAL.getCode().equals(this.status);
    }
//
//    /**
//     * 是否被禁用
//     */
//    public boolean isDisabled() {
//        return UserStatus.DISABLED.getCode().equals(this.status);
//    }
//
    /**
     * 获取显示名称（优先显示昵称，否则显示用户名）
     */
    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }

    /**
     * 获取用户类型显示名称
     */
    public String getUserTypeDisplayName() {
        try {
            return UserType.fromCode(userType).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    /**
     * 获取用户状态显示名称
     */
    public String getStatusDisplayName() {
        try {
            return UserStatus.fromCode(status).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
