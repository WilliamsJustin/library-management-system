package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.CreateReaderRequest;
import com.school.library.dto.ReaderResponse;
import com.school.library.dto.UpdateReaderRequest;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;

public interface ReaderService {

    Reader createReader(CreateReaderRequest request);

    Reader updateReader(Long id, UpdateReaderRequest request);

    /** true -> NORMAL（正常），false -> RESTRICTED（停借） */
    void toggleStatus(Long id, Boolean status);

    /** 读者列表（关键词/类型/状态组合筛选），page 为 0 基 */
    PageResult<ReaderResponse> getReaders(String keyword, ReaderType type, ReaderStatus status, int page, int size);

    Reader getReader(Long id);

    void deleteReader(Long id);

    Reader findByAccount(String account);
}
