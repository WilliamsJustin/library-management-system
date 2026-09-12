package com.school.library.dto;

import com.school.library.entity.Book;
import com.school.library.entity.BookStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "图书信息")
public class BookResponse {
    @Schema(description = "图书ID")
    private Long id;

    @Schema(description = "ISBN")
    private String isbn;

    @Schema(description = "书名")
    private String title;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "分类")
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

    @Schema(description = "状态")
    private BookStatus status;

    @Schema(description = "可借副本数")
    private Long availableCopies;

    @Schema(description = "总副本数")
    private Long totalCopies;

    public static BookResponse fromEntity(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setIsbn(book.getIsbn());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPublisher(book.getPublisher());
        response.setCategory(book.getCategory());
        response.setPublishDate(book.getPublishDate());
        response.setLanguage(book.getLanguage());
        response.setPrice(book.getPrice());
        response.setCoverUrl(book.getCoverUrl());
        response.setDescription(book.getDescription());
        response.setStatus(book.getStatus());
        return response;
    }
}
