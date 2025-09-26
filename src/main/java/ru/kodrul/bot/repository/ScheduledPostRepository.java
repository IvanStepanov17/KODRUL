package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kodrul.bot.entity.ScheduledPost;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, Long> {

    List<ScheduledPost> findByChatIdAndIsActiveTrue(Long chatId);

    List<ScheduledPost> findByChatIdAndGroupNameAndIsActiveTrue(Long chatId, String groupName);

    @Query("SELECT s FROM ScheduledPost s WHERE s.scheduledTime = :time AND s.isActive = true")
    List<ScheduledPost> findByScheduledTimeAndActive(@Param("time") LocalTime time);

    Optional<ScheduledPost> findByChatIdAndGroupNameAndScheduledTime(Long chatId, String groupName, LocalTime scheduledTime);
}