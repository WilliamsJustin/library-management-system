package com.school.library.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 读者活动（前台"读者活动"栏目展示），对所有访客可见 */
@Entity
@Table(name = "activity")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    /** 展示用时间文案，如 "9月15日 14:00" */
    @Column(nullable = false, length = 60)
    private String dateText;

    /** 活动类别标签，如 校级/培训/沙龙/活动/竞赛 */
    @Column(nullable = false, length = 20)
    private String tag;

    /** 置顶活动优先展示 */
    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Activity() {
    }

    public Activity(String title, String content, String dateText, String tag, boolean pinned) {
        this.title = title;
        this.content = content;
        this.dateText = dateText;
        this.tag = tag;
        this.pinned = pinned;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDateText() {
        return dateText;
    }

    public void setDateText(String dateText) {
        this.dateText = dateText;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
