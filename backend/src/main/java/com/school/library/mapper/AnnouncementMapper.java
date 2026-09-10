package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.library.entity.Announcement;

/**
 * 公告 Mapper。
 * 「置顶优先 + 发布时间倒序」以及标题/日期区间筛选都用 LambdaQueryWrapper 表达，
 * 无需自定义 SQL。
 */
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
