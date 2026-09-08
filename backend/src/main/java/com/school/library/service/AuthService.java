package com.school.library.service;

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
import com.school.library.repository.ReaderRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final ReaderRepository readerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(ReaderRepository readerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.readerRepository = readerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Reader reader = readerRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException(ErrorCodes.BAD_CREDENTIALS, "账号或密码错误"));

        if (!passwordEncoder.matches(request.password(), reader.getPasswordHash())) {
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
        Reader reader = readerRepository.findById(CurrentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "用户不存在"));

        if (!passwordEncoder.matches(request.oldPassword(), reader.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.OLD_PASSWORD_MISMATCH, "原密码错误");
        }
        reader.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    /** 读者自助注册（匿名开放），注册成功即自动登录 */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (readerRepository.existsByAccount(request.getAccount())) {
            throw new BusinessException(ErrorCodes.CONFLICT, "该账号已被注册");
        }
        if (readerRepository.existsByStudentNo(request.getStudentNo())) {
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
        reader = readerRepository.save(reader);

        AppPrincipal principal = new AppPrincipal(
                reader.getId(), reader.getAccount(), reader.getRole(), reader.getType().name());
        String token = jwtService.generate(principal);
        return new LoginResponse(token, reader.getId(), reader.getName(),
                reader.getRole().name(), reader.getType().name());
    }
}
