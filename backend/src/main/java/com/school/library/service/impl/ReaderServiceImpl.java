package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.CreateReaderRequest;
import com.school.library.dto.ReaderResponse;
import com.school.library.dto.UpdateReaderRequest;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.exception.ConflictException;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.ReaderMapper;
import com.school.library.service.ReaderService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReaderServiceImpl implements ReaderService {

    private final ReaderMapper readerMapper;
    private final LoanMapper loanMapper;
    private final PasswordEncoder passwordEncoder;

    public ReaderServiceImpl(ReaderMapper readerMapper, LoanMapper loanMapper, PasswordEncoder passwordEncoder) {
        this.readerMapper = readerMapper;
        this.loanMapper = loanMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Reader createReader(CreateReaderRequest request) {
        if (accountExists(request.getAccount())) {
            throw new ConflictException("账号已存在");
        }
        if (studentNoExists(request.getStudentNo())) {
            throw new ConflictException("学号/工号已存在");
        }

        Reader reader = new Reader();
        reader.setAccount(request.getAccount());
        reader.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        reader.setName(request.getName());
        reader.setRole(UserRole.READER);
        reader.setType(request.getType());
        reader.setStudentNo(request.getStudentNo());
        reader.setStatus(ReaderStatus.NORMAL);

        readerMapper.insert(reader);
        return reader;
    }

    @Override
    @Transactional
    public Reader updateReader(Long id, UpdateReaderRequest request) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new NotFoundException("读者不存在");
        }

        // 如果更新账号，检查是否已存在（排除自己）
        if (request.getAccount() != null && !request.getAccount().equals(reader.getAccount())) {
            if (countByAccount(request.getAccount(), id) > 0) {
                throw new ConflictException("账号已存在");
            }
            reader.setAccount(request.getAccount());
        }

        if (request.getStudentNo() != null && !request.getStudentNo().equals(reader.getStudentNo())) {
            if (countByStudentNo(request.getStudentNo(), id) > 0) {
                throw new ConflictException("学号/工号已存在");
            }
            reader.setStudentNo(request.getStudentNo());
        }

        if (request.getName() != null) {
            reader.setName(request.getName());
        }
        if (request.getType() != null) {
            reader.setType(request.getType());
        }

        readerMapper.updateById(reader);
        return reader;
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Boolean status) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new NotFoundException("读者不存在");
        }

        reader.setStatus(Boolean.TRUE.equals(status) ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED);
        readerMapper.updateById(reader);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReaderResponse> getReaders(String keyword, ReaderType type, ReaderStatus status,
                                                 int page, int size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<Reader> wrapper = Wrappers.<Reader>lambdaQuery()
                // 三个关键词字段用 OR 归到一组，避免与外层的 type/status 条件混淆优先级
                .and(hasKeyword, w -> w.like(Reader::getName, keyword)
                        .or().like(Reader::getAccount, keyword)
                        .or().like(Reader::getStudentNo, keyword))
                .eq(type != null, Reader::getType, type)
                .eq(status != null, Reader::getStatus, status)
                .orderByAsc(Reader::getId);

        return PageResult.of(readerMapper.selectPage(Pages.of(page, size), wrapper), ReaderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Reader getReader(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new NotFoundException("读者不存在");
        }
        return reader;
    }

    @Override
    @Transactional
    public void deleteReader(Long id) {
        if (readerMapper.selectById(id) == null) {
            throw new NotFoundException("读者不存在");
        }

        // 检查是否有未归还的借阅
        long activeLoans = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getReaderId, id)
                .in(Loan::getStatus, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE)));
        if (activeLoans > 0) {
            throw new ConflictException("读者有未归还的图书，无法删除");
        }

        readerMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Reader findByAccount(String account) {
        Reader reader = readerMapper.selectOne(Wrappers.<Reader>lambdaQuery().eq(Reader::getAccount, account));
        if (reader == null) {
            throw new NotFoundException("读者不存在");
        }
        return reader;
    }

    private boolean accountExists(String account) {
        return countByAccount(account, null) > 0;
    }

    private boolean studentNoExists(String studentNo) {
        return countByStudentNo(studentNo, null) > 0;
    }

    /** 统计同账号的读者数；excludeId 不为 null 时排除该读者自身（用于更新场景） */
    private long countByAccount(String account, Long excludeId) {
        return readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getAccount, account)
                .ne(excludeId != null, Reader::getId, excludeId));
    }

    /** 统计同学号的读者数；excludeId 不为 null 时排除该读者自身（用于更新场景） */
    private long countByStudentNo(String studentNo, Long excludeId) {
        return readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getStudentNo, studentNo)
                .ne(excludeId != null, Reader::getId, excludeId));
    }
}
