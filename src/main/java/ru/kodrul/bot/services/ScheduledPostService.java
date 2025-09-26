package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.repository.ScheduledPostRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPostService {

    private final ScheduledPostRepository scheduledPostRepository;

    @Transactional
    public ScheduledPost createSchedule(Long chatId, String groupName, LocalTime scheduledTime,
                                        String messageText, String imageUrl, Long createdBy) {

        Optional<ScheduledPost> existing = scheduledPostRepository.findByChatIdAndGroupNameAndScheduledTime(
                chatId, groupName, scheduledTime);

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Расписание для этой группы в это время уже существует");
        }

        ScheduledPost schedule = new ScheduledPost();
        schedule.setChatId(chatId);
        schedule.setGroupName(groupName);
        schedule.setScheduledTime(scheduledTime);
        schedule.setMessageText(messageText);
        schedule.setImageUrl(imageUrl);
        schedule.setCreatedBy(createdBy);
        schedule.setIsActive(true);

        ScheduledPost saved = scheduledPostRepository.save(schedule);
        log.info("Created scheduled post: {} for chat {} at {}", groupName, chatId, scheduledTime);
        return saved;
    }

    @Transactional
    public ScheduledPost updateSchedule(Long scheduleId, LocalTime newTime, String newMessage, String newImageUrl) {
        ScheduledPost schedule = scheduledPostRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Расписание не найдено"));

        schedule.setScheduledTime(newTime);
        schedule.setMessageText(newMessage);
        schedule.setImageUrl(newImageUrl);

        return scheduledPostRepository.save(schedule);
    }

    @Transactional
    public void toggleSchedule(Long scheduleId, boolean isActive) {
        ScheduledPost schedule = scheduledPostRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Расписание не найдено"));

        schedule.setIsActive(isActive);
        scheduledPostRepository.save(schedule);
        log.info("Schedule {} {}", scheduleId, isActive ? "activated" : "deactivated");
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        if (!scheduledPostRepository.existsById(scheduleId)) {
            throw new IllegalArgumentException("Расписание не найдено");
        }
        scheduledPostRepository.deleteById(scheduleId);
        log.info("Deleted schedule: {}", scheduleId);
    }

    public List<ScheduledPost> getActiveSchedulesForChat(Long chatId) {
        return scheduledPostRepository.findByChatIdAndIsActiveTrue(chatId);
    }

    public List<ScheduledPost> getSchedulesByTime(LocalTime time) {
        return scheduledPostRepository.findByScheduledTimeAndActive(time);
    }

    @Transactional
    public void markAsSent(Long scheduleId) {
        ScheduledPost schedule = scheduledPostRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Расписание не найдено"));

        schedule.setLastSent(LocalDateTime.now());
        scheduledPostRepository.save(schedule);
    }
}