package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.CreateReaderRequest;
import com.school.library.dto.ExcelImportResult;
import com.school.library.dto.ReaderResponse;
import com.school.library.dto.UpdateReaderRequest;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ConflictException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.ReaderMapper;
import com.school.library.service.ReaderService;
import com.school.library.util.ExcelTemplateUtil;
import com.school.library.util.ImportValidator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReaderServiceImpl implements ReaderService {

    private final ReaderMapper readerMapper;
    private final LoanMapper loanMapper;
    private final PasswordEncoder passwordEncoder;

    /** 导出/导入模板共享定义：表头、列宽、文件名（导出文件与导入模板同款式，密码列恒为空不外泄） */
    public static final List<String> EXPORT_HEADERS =
            List.of("账号", "密码", "姓名", "类型", "学号/工号", "手机号");
    public static final int[] EXPORT_WIDTHS = {18, 14, 12, 10, 16, 16};
    public static final String TEMPLATE_FILENAME = "读者导入模板.xlsx";
    public static final String EXPORT_FILENAME = "读者导出.xlsx";

    private static String[] toExportRow(Reader r) {
        return new String[]{
                r.getAccount() == null ? "" : r.getAccount(),
                "",
                r.getName() == null ? "" : r.getName(),
                r.getType() == ReaderType.TEACHER ? "教师" : "学生",
                r.getStudentNo() == null ? "" : r.getStudentNo(),
                r.getPhone() == null ? "" : r.getPhone()
        };
    }

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
        reader.setPhone(request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim());
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
        // 手机号：传值则更新（空串表示清除）
        if (request.getPhone() != null) {
            reader.setPhone(request.getPhone().isBlank() ? null : request.getPhone().trim());
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
    public List<Long> listReaderIds(String keyword, ReaderType type, ReaderStatus status) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<Reader> wrapper = Wrappers.<Reader>lambdaQuery()
                .and(hasKeyword, w -> w.like(Reader::getName, keyword)
                        .or().like(Reader::getAccount, keyword)
                        .or().like(Reader::getStudentNo, keyword))
                .eq(type != null, Reader::getType, type)
                .eq(status != null, Reader::getStatus, status)
                .orderByAsc(Reader::getId);
        return readerMapper.selectList(wrapper).stream().map(Reader::getId).toList();
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

    @Override
    @Transactional
    public void resetPassword(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new NotFoundException("读者不存在");
        }
        // 密码重置只面向读者账号，避免误改管理员
        if (reader.getRole() != UserRole.READER) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只能重置读者账号的密码");
        }
        reader.setPasswordHash(passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
        readerMapper.updateById(reader);
    }

    @Override
    @Transactional
    public int batchResetPassword(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // 只更新读者角色行；单条 SQL 完成（管理员账号天然被排除）
        return readerMapper.update(null, Wrappers.<Reader>lambdaUpdate()
                .in(Reader::getId, ids)
                .eq(Reader::getRole, UserRole.READER)
                .set(Reader::getPasswordHash, passwordEncoder.encode(DEFAULT_RESET_PASSWORD)));
    }

    @Override
    @Transactional
    public int batchUpdateStatus(List<Long> ids, Boolean status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        ReaderStatus next = Boolean.TRUE.equals(status) ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED;
        return readerMapper.update(null, Wrappers.<Reader>lambdaUpdate()
                .in(Reader::getId, ids)
                .eq(Reader::getRole, UserRole.READER)
                .set(Reader::getStatus, next));
    }

    @Override
    @Transactional
    public ExcelImportResult importReaders(MultipartFile file) {
        ImportValidator.checkSize(file);
        Set<String> accountsInFile = new HashSet<>();
        Set<String> studentNosInFile = new HashSet<>();
        List<String> errors = new ArrayList<>();
        int success = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = Math.min(sheet.getLastRowNum(), ImportValidator.MAX_ROWS);
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int line = i + 1; // 给用户看的行号从 1 开始且含表头

                String account = text(row.getCell(0));
                String password = text(row.getCell(1));
                String name = text(row.getCell(2));
                String typeText = text(row.getCell(3));
                String studentNo = text(row.getCell(4));
                String phone = text(row.getCell(5));

                if (account.isEmpty()) {
                    errors.add("第 " + line + " 行：账号为空，已跳过");
                    continue;
                }
                if (name.isEmpty()) {
                    errors.add("第 " + line + " 行：姓名为空，已跳过");
                    continue;
                }
                ReaderType type = parseType(typeText);
                if (type == null) {
                    errors.add("第 " + line + " 行：类型应为「学生」或「教师」，已跳过");
                    continue;
                }
                if (accountsInFile.contains(account)
                        || readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                        .eq(Reader::getAccount, account)) > 0) {
                    errors.add("第 " + line + " 行：账号「" + account + "」已存在，已跳过");
                    continue;
                }
                if (!studentNo.isEmpty() && (studentNosInFile.contains(studentNo)
                        || countByStudentNo(studentNo, null) > 0)) {
                    errors.add("第 " + line + " 行：学号/工号「" + studentNo + "」已存在，已跳过");
                    continue;
                }

                Reader reader = new Reader();
                reader.setAccount(account);
                reader.setPasswordHash(passwordEncoder.encode(
                        password.isEmpty() ? DEFAULT_RESET_PASSWORD : password));
                reader.setName(name);
                reader.setRole(UserRole.READER);
                reader.setType(type);
                reader.setStudentNo(studentNo.isEmpty() ? null : studentNo);
                reader.setPhone(phone.isEmpty() ? null : phone);
                reader.setStatus(ReaderStatus.NORMAL);
                readerMapper.insert(reader);

                accountsInFile.add(account);
                if (!studentNo.isEmpty()) {
                    studentNosInFile.add(studentNo);
                }
                success++;
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "文件读取失败，请确认是有效的 Excel 文件");
        }
        return ExcelImportResult.of(success, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReaders(String scope, String keyword, ReaderType type, ReaderStatus status,
                                int page, int size, List<Long> ids) {
        String sc = scope == null ? "all" : scope;
        List<Reader> list;
        switch (sc) {
            case "page" -> {
                PageResult<ReaderResponse> pr = getReaders(keyword, type, status, page, size);
                list = pr.getContent().stream()
                        .map(resp -> readerMapper.selectById(resp.getId()))
                        .filter(java.util.Objects::nonNull)
                        .toList();
            }
            case "selected" -> list = ids == null ? List.of() : ids.stream()
                    .map(readerMapper::selectById)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            default -> list = readerMapper.selectList(Wrappers.<Reader>lambdaQuery()
                    .and(keyword != null && !keyword.isBlank(), w -> w.like(Reader::getName, keyword)
                            .or().like(Reader::getAccount, keyword)
                            .or().like(Reader::getStudentNo, keyword))
                    .eq(type != null, Reader::getType, type)
                    .eq(status != null, Reader::getStatus, status)
                    .orderByAsc(Reader::getId));
        }
        return ExcelTemplateUtil.build(
                EXPORT_HEADERS, list.stream().map(ReaderServiceImpl::toExportRow).toList(), EXPORT_WIDTHS);
    }

    /** 类型列：学生 / 教师（兼容 STUDENT / TEACHER），不识别返回 null */
    private ReaderType parseType(String text) {
        if (text.contains("教师") || "TEACHER".equalsIgnoreCase(text)) {
            return ReaderType.TEACHER;
        }
        if (text.contains("学生") || "STUDENT".equalsIgnoreCase(text)) {
            return ReaderType.STUDENT;
        }
        return null;
    }

    /** 读取单元格为去空格字符串（数字单元格避免科学计数法） */
    private String text(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula() == null ? "" : cell.getCellFormula().trim();
            default -> "";
        };
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
