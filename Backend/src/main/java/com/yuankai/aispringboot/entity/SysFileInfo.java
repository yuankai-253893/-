package com.yuankai.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_file_info")
public class SysFileInfo {
    // 文件ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 原始文件名（用户上传时的文件名）
    @TableField("original_name")
    private String originalName;

    // 文件访问路径
    @TableField("file_path")
    private String filePath;

    // 文件大小（单位：字节）
    @TableField("file_size")
    private Long fileSize;

    // 文件类型（IMG/PDF/TXT/DOC/XLS等）
    @TableField("file_type")
    private String fileType;

    // 业务类型（用于区分文件用途，如：avatar/document/attachment）
    @TableField("business_type")
    private String businessType;

    // 业务对象ID（关联的业务数据主键）
    @TableField("business_id")
    private String businessId;

    // 业务字段名（对应业务表中的字段名）
    @TableField("business_field")
    private String businessField;

    // 上传用户ID
    @TableField("upload_user_id")
    private Long uploadUserId;

    // 是否临时文件（1:是 0:否）
    @TableField("is_temp")
    private Integer isTemp;

    // 文件状态（0:删除 1:正常）
    private Integer status;

    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 过期时间（仅对临时文件有效）
    @TableField("expire_time")
    private LocalDateTime expireTime;
}
