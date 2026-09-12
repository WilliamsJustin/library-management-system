package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Excel 批量导入结果（成功/失败计数 + 逐行错误明细） */
@Schema(description = "Excel 批量导入结果")
public record ExcelImportResult(
        @Schema(description = "读取到的数据行数")
        int total,

        @Schema(description = "成功导入条数")
        int success,

        @Schema(description = "失败条数")
        int failed,

        @Schema(description = "失败原因明细（含行号）")
        List<String> errors
) {
    public static ExcelImportResult of(int success, List<String> errors) {
        int failed = errors.size();
        return new ExcelImportResult(success + failed, success, failed, errors);
    }
}
