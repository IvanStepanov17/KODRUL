package ru.kodrul.bot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "telegram_users")
public class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "is_bot")
    private Boolean isBot = false;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GroupMember> groupMemberships = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        firstSeen = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }
}