package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kodrul.bot.entity.ScheduledPost;

import java.util.List;

public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, Long> {

    List<ScheduledPost> findByChatIdAndIsActiveTrue(Long chatId);

    List<ScheduledPost> findByIsActiveTrue();

    @Query("SELECT s FROM ScheduledPost s " +
                "WHERE s.chatId = :chatId " +
                    "AND s.groupName = :groupName AND s.isActive = true")
    List<ScheduledPost> findByChatIdAndGroupNameAndIsActiveTrue(
            @Param("chatId") Long chatId,
            @Param("groupName") String groupName);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM ScheduledPost s " +
                "WHERE s.chatId = :chatId " +
                    "AND (s.messageThreadId = :threadId OR (s.messageThreadId IS NULL AND :threadId IS NULL)) " +
                    "AND s.groupName = :groupName " +
                    "AND s.cronExpression = :cronExpression")
    boolean existsByChatIdAndThreadIdAndGroupNameAndCronExpression(
            @Param("chatId") Long chatId,
            @Param("threadId") Integer threadId,
            @Param("groupName") String groupName,
            @Param("cronExpression") String cronExpression);

    @Query("SELECT s FROM ScheduledPost s " +
                "WHERE s.chatId = :chatId " +
                    "AND (s.messageThreadId = :threadId OR :threadId IS NULL) " +
                    "AND s.isActive = true")
    List<ScheduledPost> findByChatIdAndThreadIdAndIsActiveTrue(
            @Param("chatId") Long chatId,
            @Param("threadId") Integer threadId);

    @Query("SELECT s FROM ScheduledPost s " +
                "WHERE s.chatId = :chatId " +
                    "AND s.groupName = :groupName " +
                    "AND (s.messageThreadId = :threadId OR :threadId IS NULL) " +
                    "AND s.isActive = true")
    List<ScheduledPost> findByChatIdAndGroupNameAndThreadIdAndIsActiveTrue(
            @Param("chatId") Long chatId,
            @Param("groupName") String groupName,
            @Param("threadId") Integer threadId);
}