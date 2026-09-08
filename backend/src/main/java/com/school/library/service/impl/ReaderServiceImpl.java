package com.school.library.service.impl;

import com.school.library.dto.CreateReaderRequest;
import com.school.library.dto.UpdateReaderRequest;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import com.school.library.exception.ConflictException;
import com.school.library.exception.NotFoundException;
import com.school.library.repository.LoanRepository;
import com.school.library.repository.ReaderRepository;
import com.school.library.service.ReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReaderServiceImpl implements ReaderService {

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Reader createReader(CreateReaderRequest request) {
        // 检查账号是否已存在
        if (readerRepository.existsByAccount(request.getAccount())) {
            throw new ConflictException("账号已存在");
        }
        if (readerRepository.existsByStudentNo(request.getStudentNo())) {
            throw new ConflictException("学号/工号已存在");
        }

        Reader reader = new Reader();
        reader.setAccount(request.getAccount());
        reader.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        reader.setName(request.getName());
        reader.setRole(com.school.library.entity.UserRole.READER);
        reader.setType(request.getType());
        reader.setStudentNo(request.getStudentNo());
        reader.setStatus(ReaderStatus.NORMAL);

        return readerRepository.save(reader);
    }

    @Override
    @Transactional
    public Reader updateReader(Long id, UpdateReaderRequest request) {
        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("读者不存在"));

        // 如果更新账号，检查是否已存在
        if (request.getAccount() != null && !request.getAccount().equals(reader.getAccount())) {
            if (readerRepository.existsByAccountAndIdNot(request.getAccount(), id)) {
                throw new ConflictException("账号已存在");
            }
            reader.setAccount(request.getAccount());
        }

        if (request.getStudentNo() != null && !request.getStudentNo().equals(reader.getStudentNo())) {
            if (readerRepository.existsByStudentNoAndIdNot(request.getStudentNo(), id)) {
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

        return readerRepository.save(reader);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Boolean status) {
        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("读者不存在"));

        reader.setStatus(Boolean.TRUE.equals(status) ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED);
        readerRepository.save(reader);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Reader> getReaders(String keyword, ReaderType type, ReaderStatus status, Pageable pageable) {
        return readerRepository.query(keyword, type, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Reader getReader(Long id) {
        return readerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("读者不存在"));
    }

    @Override
    @Transactional
    public void deleteReader(Long id) {
        readerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("读者不存在"));

        // 检查是否有未归还的借阅
        if (loanRepository.existsByReaderIdAndStatusIn(id, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))) {
            throw new ConflictException("读者有未归还的图书，无法删除");
        }

        readerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Reader findByAccount(String account) {
        return readerRepository.findByAccount(account)
                .orElseThrow(() -> new NotFoundException("读者不存在"));
    }
}
