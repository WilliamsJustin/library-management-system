package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.CreateReaderRequest;
import com.school.library.dto.ExcelImportResult;
import com.school.library.dto.ReaderResponse;
import com.school.library.dto.UpdateReaderRequest;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReaderService {

    Reader createReader(CreateReaderRequest request);

    Reader updateReader(Long id, UpdateReaderRequest request);

    /** true -> NORMAL（正常），false -> RESTRICTED（停借） */
    void toggleStatus(Long id, Boolean status);

    /** 读者列表（关键词/类型/状态组合筛选），page 为 0 基 */
    PageResult<ReaderResponse> getReaders(String keyword, ReaderType type, ReaderStatus status, int page, int size);

    /** 符合筛选条件的全部读者 ID（跨页全选用），与列表接口同一套筛选参数 */
    List<Long> listReaderIds(String keyword, ReaderType type, ReaderStatus status);

    Reader getReader(Long id);

    void deleteReader(Long id);

    Reader findByAccount(String account);

    /** 重置后的初始密码提示用（与种子数据一致） */
    String DEFAULT_RESET_PASSWORD = "pass123";

    /** 重置密码（管理员）：恢复为初始密码，仅对读者角色生效 */
    void resetPassword(Long id);

    /** 批量重置密码（管理员），返回实际重置的条数 */
    int batchResetPassword(List<Long> ids);

    /** 批量设置状态（true 正常 / false 停借），返回实际更新的条数 */
    int batchUpdateStatus(List<Long> ids, Boolean status);

    /**
     * Excel 批量导入读者。
     * 列顺序：账号 | 密码 | 姓名 | 类型（学生/教师） | 学号/工号 | 手机号（可空）。
     * 密码留空时使用初始密码；账号/学号重复的行跳过并记入错误明细。
     */
    ExcelImportResult importReaders(MultipartFile file);

    /** 导出读者为 xlsx（scope: all 全部 / page 单页 / selected 选中），文件内容与导入模板同款式；密码列恒为空 */
    byte[] exportReaders(String scope, String keyword, ReaderType type, ReaderStatus status,
                         int page, int size, List<Long> ids);
}
