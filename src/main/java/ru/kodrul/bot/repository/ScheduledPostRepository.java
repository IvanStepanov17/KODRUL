package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kodrul.bot.entity.ScheduledPost;

import java.util.List;

public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, Long> {

    List<ScheduledPost> findByChatIdAndIsActiveTrue(Long chatId);

    List<ScheduledPost> findByIsActiveTrue();

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ScheduledPost s WHERE s.chatId = :chatId AND s.groupName = :groupName AND s.cronExpression = :cronExpression")
    boolean existsByChatIdAndGroupNameAndCronExpression(
            @Param("chatId") Long chatId,
            @Param("groupName") String groupName,
            @Param("cronExpression") String cronExpression);

    @Query("SELECT s FROM ScheduledPost s WHERE s.chatId = :chatId AND s.groupName = :groupName AND s.isActive = true")
    List<ScheduledPost> findByChatIdAndGroupNameAndIsActiveTrue(
            @Param("chatId") Long chatId,
            @Param("groupName") String groupName);

    // Потенциально для каких-то дополнительных функций (например, при показе расписаний для конкретной группы)
    @Query("SELECT s FROM ScheduledPost s WHERE s.chatId = :chatId AND s.groupName = :groupName AND s.isActive = true")
    List<ScheduledPost> findActiveByChatAndGroup(@Param("chatId") Long chatId, @Param("groupName") String groupName);
}