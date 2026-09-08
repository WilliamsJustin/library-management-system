package com.school.library.service;

import com.school.library.dto.CreateReaderRequest;
import com.school.library.dto.UpdateReaderRequest;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReaderService {

    Reader createReader(CreateReaderRequest request);

    Reader updateReader(Long id, UpdateReaderRequest request);

    /** true -> NORMAL（正常），false -> RESTRICTED（停借） */
    void toggleStatus(Long id, Boolean status);

    Page<Reader> getReaders(String keyword, ReaderType type, ReaderStatus status, Pageable pageable);

    Reader getReader(Long id);

    void deleteReader(Long id);

    Reader findByAccount(String account);
}
