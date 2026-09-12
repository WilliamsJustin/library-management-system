package com.school.library.dto;

import com.school.library.entity.ReaderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建读者请求")
public class CreateReaderRequest {

    @Schema(description = "账号")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "姓名")
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(description = "读者类型")
    @NotNull(message = "读者类型不能为空")
    private ReaderType type;

    @Schema(description = "学号/工号")
    @NotBlank(message = "学号/工号不能为空")
    private String studentNo;

    @Schema(description = "手机号（可选，11 位大陆手机号）")
    @Size(max = 30, message = "手机号过长")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}