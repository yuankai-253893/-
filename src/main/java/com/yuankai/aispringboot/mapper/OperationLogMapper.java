package com.yuankai.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuankai.aispringboot.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 * 由 AOP 切面自动写入，暂无查询接口（可按需扩展）
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
