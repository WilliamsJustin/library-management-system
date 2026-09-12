package com.school.library.service;

import com.school.library.dto.DashboardStatsResponse;

/** 管理员首页仪表盘统计 */
public interface StatsService {

    /** 汇总借阅流通 / 逾期罚款 / 实时咨询三块概况 */
    DashboardStatsResponse overview();
}
