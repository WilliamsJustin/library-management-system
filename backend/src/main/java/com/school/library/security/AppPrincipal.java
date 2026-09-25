package com.school.library.security;

import com.school.library.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 认证主体：登录成功后放入 SecurityContext。
 *
 * 注意：认证已由「JWT 无状态」改为「服务端会话」，本对象会随 SecurityContext 一起写进 HttpSession，
 * 而 Spring Session 的 RedisSessionRepository 默认使用 JDK 序列化，所以它必须实现 Serializable，
 * 且所有字段类型都要可序列化（Long / String / 枚举都满足），否则登录时会抛 NotSerializableException。
 */
public record AppPrincipal(Long userId, String account, String name, UserRole role, String readerType)
        implements UserDetails, Serializable {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return account;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
