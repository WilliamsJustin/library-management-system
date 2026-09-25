package com.school.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.dto.ChangePasswordRequest;
import com.school.library.dto.LoginRequest;
import com.school.library.dto.RegisterRequest;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.mapper.ReaderMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务。
 *
 * 只负责「校验账号密码 / 创建读者账号」并返回认证主体；
 * 会话的建立与销毁属于 HTTP 交互，放在 security/LoginSessionManager，由 AuthController 串联。
 * （认证已从 JWT 改为 Spring Session 服务端会话，本类不再签发令牌。）
 */
@Service
public class AuthService {

    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(ReaderMapper readerMapper, PasswordEncoder passwordEncoder) {
        this.readerMapper = readerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 校验账号密码，成功则返回认证主体（由调用方建立会话） */
    @Transactional(readOnly = true)
    public AppPrincipal authenticate(LoginRequest request) {
        Reader reader = readerMapper.selectOne(
                Wrappers.<Reader>lambdaQuery().eq(Reader::getAccount, request.account()));
        if (reader == null || !passwordEncoder.matches(request.password(), reader.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.BAD_CREDENTIALS, "账号或密码错误");
        }
        return toPrincipal(reader);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Reader reader = readerMapper.selectById(CurrentUser.userId());
        if (reader == null) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "用户不存在");
        }

        if (!passwordEncoder.matches(request.oldPassword(), reader.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.OLD_PASSWORD_MISMATCH, "原密码错误");
        }
        // MyBatis-Plus 没有 JPA 的脏检查，改完必须显式 update
        reader.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        readerMapper.updateById(reader);
    }

    /** 读者自助注册（匿名开放），成功即返回认证主体供调用方建立会话（自动登录） */
    @Transactional
    public AppPrincipal register(RegisterRequest request) {
        if (readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getAccount, request.getAccount())) > 0) {
            throw new BusinessException(ErrorCodes.CONFLICT, "该账号已被注册");
        }
        if (readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getStudentNo, request.getStudentNo())) > 0) {
            throw new BusinessException(ErrorCodes.CONFLICT, "该学号/工号已被使用");
        }

        Reader reader = new Reader();
        reader.setAccount(request.getAccount());
        reader.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        reader.setName(request.getName());
        reader.setRole(UserRole.READER);
        reader.setType(request.getType() == null ? ReaderType.STUDENT : request.getType());
        reader.setStudentNo(request.getStudentNo());
        reader.setPhone(request.getPhone());
        reader.setStatus(ReaderStatus.NORMAL);
        readerMapper.insert(reader);

        return toPrincipal(reader);
    }

    private AppPrincipal toPrincipal(Reader reader) {
        return new AppPrincipal(reader.getId(), reader.getAccount(), reader.getName(),
                reader.getRole(), reader.getType().name());
    }
}
