package com.school.library.dto;

import com.school.library.entity.ReaderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "读者自助注册请求（匿名开放）")
public class RegisterRequest {

    @Schema(description = "登录账号")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度需在 3-50 位之间")
    private String account;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间")
    private String password;

    @Schema(description = "姓名")
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(description = "读者类型：STUDENT 学生 / TEACHER 教师，默认 STUDENT")
    private ReaderType type = ReaderType.STUDENT;

    @Schema(description = "学号或工号")
    @NotBlank(message = "学号/工号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "学号/工号只能包含字母、数字与连字符")
    private String studentNo;

    @Schema(description = "手机号（可选）")
    private String phone;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReaderType getType() {
        return type;
    }

    public void setType(ReaderType type) {
        this.type = type;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
