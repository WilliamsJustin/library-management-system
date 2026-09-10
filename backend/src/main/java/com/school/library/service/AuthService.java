package com.school.library.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.dto.ChangePasswordRequest;
import com.school.library.dto.LoginRequest;
import com.school.library.dto.LoginResponse;
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
import com.school.library.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(ReaderMapper readerMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.readerMapper = readerMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Reader reader = readerMapper.selectOne(
                Wrappers.<Reader>lambdaQuery().eq(Reader::getAccount, request.account()));
        if (reader == null || !passwordEncoder.matches(request.password(), reader.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.BAD_CREDENTIALS, "账号或密码错误");
        }

        AppPrincipal principal = new AppPrincipal(
                reader.getId(), reader.getAccount(), reader.getRole(), reader.getType().name());
        String token = jwtService.generate(principal);
        return new LoginResponse(token, reader.getId(), reader.getName(),
                reader.getRole().name(), reader.getType().name());
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

    /** 读者自助注册（匿名开放），注册成功即自动登录 */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
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

        AppPrincipal principal = new AppPrincipal(
                reader.getId(), reader.getAccount(), reader.getRole(), reader.getType().name());
        String token = jwtService.generate(principal);
        return new LoginResponse(token, reader.getId(), reader.getName(),
                reader.getRole().name(), reader.getType().name());
    }
}
