package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kodrul.bot.entity.TelegramUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByUserId(Long userId);

    @Query("SELECT u FROM TelegramUser u WHERE u.lastSeen < :cutoffDate")
    List<TelegramUser> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE TelegramUser u SET u.lastSeen = CURRENT_TIMESTAMP WHERE u.userId = :userId")
    void updateLastSeen(@Param("userId") Long userId);

    Optional<TelegramUser> findByUserName(String userName);

    @Query("SELECT DISTINCT gm.group.chatId FROM GroupMember gm")
    List<Long> findDistinctChatIds();
}