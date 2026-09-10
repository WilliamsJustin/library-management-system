package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 图书副本（可借实体，对应表 book_copy） */
@Data
@TableName("book_copy")
public class BookCopy {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属图书 ID —— 取代原 JPA 的 {@code @ManyToOne Book book} 关联对象 */
    @TableField("book_id")
    private Long bookId;

    @NotBlank
    private String barcode;

    private String location;

    private CopyStatus status = CopyStatus.IN_STOCK;

    /** 乐观锁版本，防止同一副本被并发借出（等价于原 JPA 的 @Version，由 OptimisticLockerInnerInterceptor 接管） */
    @Version
    private Long version;
}
