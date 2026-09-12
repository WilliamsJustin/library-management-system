package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 收藏状态（收藏 / 取消收藏后返回，前端据此更新按钮） */
@Data
@Schema(description = "收藏状态")
public class FavoriteStateResponse {

    @Schema(description = "图书ID")
    private Long bookId;

    @Schema(description = "当前是否已收藏")
    private boolean favorited;

    public FavoriteStateResponse(Long bookId, boolean favorited) {
        this.bookId = bookId;
        this.favorited = favorited;
    }
}
