package com.school.library.util;

import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import org.springframework.web.multipart.MultipartFile;

/** Excel 导入的公共限制（与前端「上传 Excel 文件」弹窗提示保持一致） */
public final class ImportValidator {

    /** 文件大小上限 2MB */
    public static final long MAX_SIZE = 2 * 1024 * 1024L;

    /** 每次最多导入的数据行数 */
    public static final int MAX_ROWS = 10_000;

    private ImportValidator() {
    }

    public static void checkSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请选择要导入的 Excel 文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "仅支持后缀名为 xls 或 xlsx 的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "文件大小请勿超过 2MB");
        }
    }
}
