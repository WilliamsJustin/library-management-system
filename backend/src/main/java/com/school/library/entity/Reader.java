package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 读者（含管理员账号，role = ADMIN，对应表 reader） */
@Data
@TableName("reader")
public class Reader {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    @NotBlank
    private String account;

    /** BCrypt 密码哈希（列名 password_hash） */
    @TableField("password_hash")
    private String passwordHash;

    @NotBlank
    private String name;

    private UserRole role = UserRole.READER;

    private ReaderType type = ReaderType.STUDENT;

    /** 学号或工号（列名 student_no） */
    @TableField("student_no")
    private String studentNo;

    private String phone;

    private ReaderStatus status = ReaderStatus.NORMAL;
}
