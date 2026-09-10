package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.library.entity.Reader;

/**
 * 读者 Mapper。
 * 账号/学号唯一性校验、关键词+类型+状态组合查询都能用 LambdaQueryWrapper 表达，
 * 因此这里不需要任何自定义方法。
 */
public interface ReaderMapper extends BaseMapper<Reader> {
}
