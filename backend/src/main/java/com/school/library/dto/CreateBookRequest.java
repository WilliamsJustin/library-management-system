package com.school.library.dto;

import com.school.library.entity.BookStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "创建图书请求")
public class CreateBookRequest {

    @Schema(description = "ISBN")
    @NotBlank(message = "ISBN不能为空")
    private String isbn;

    @Schema(description = "书名")
    @NotBlank(message = "书名不能为空")
    private String title;

    @Schema(description = "作者")
    @NotBlank(message = "作者不能为空")
    private String author;

    @Schema(description = "出版社")
    @NotBlank(message = "出版社不能为空")
    private String publisher;

    @Schema(description = "分类")
    @NotBlank(message = "分类不能为空")
    private String category;

    @Schema(description = "出版日期")
    private LocalDate publishDate;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "定价（元）")
    private BigDecimal price;

    @Schema(description = "封面图片地址")
    private String coverUrl;

    @Schema(description = "内容简介")
    private String description;
}