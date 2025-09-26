package ru.kodrul.bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "scheduled_posts")
public class ScheduledPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime; // Время отправки (например, 09:00)

    @Column(name = "message_text", length = 1000)
    private String messageText;

    @Column(name = "image_url")
    private String imageUrl; // URL изображения (может быть null)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_sent")
    private LocalDateTime lastSent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}