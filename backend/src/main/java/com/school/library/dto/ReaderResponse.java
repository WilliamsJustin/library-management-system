package com.school.library.dto;

import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "读者信息")
public class ReaderResponse {
    @Schema(description = "读者ID")
    private Long id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "读者类型")
    private ReaderType type;

    @Schema(description = "学号/工号")
    private String studentNo;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态：NORMAL 正常 / RESTRICTED 受限（停借）")
    private ReaderStatus status;

    public static ReaderResponse fromEntity(Reader reader) {
        ReaderResponse response = new ReaderResponse();
        response.setId(reader.getId());
        response.setAccount(reader.getAccount());
        response.setName(reader.getName());
        response.setType(reader.getType());
        response.setStudentNo(reader.getStudentNo());
        response.setPhone(reader.getPhone());
        response.setStatus(reader.getStatus());
        return response;
    }
}
