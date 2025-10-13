package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.pojo.CronParseResult;
import ru.kodrul.bot.repository.ScheduledPostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPostService {

    private final ScheduledPostRepository scheduledPostRepository;
    private final CronService cronService;

    @Transactional
    public ScheduledPost createSchedule(
            Long chatId,
            String groupName,
            String scheduleInput,
            String messageText,
            String imageUrl,
            Long createdBy)
    {

        CronParseResult cronResult = cronService.parseCronExpression(scheduleInput);

        if (!cronResult.isSuccess()) {
            throw new IllegalArgumentException("Неверный формат расписания: " + scheduleInput);
        }

        if (scheduledPostRepository.existsByChatIdAndGroupNameAndCronExpression(
                chatId, groupName, cronResult.getCronExpression())) {
            throw new IllegalArgumentException("Такое расписание уже существует для этой группы");
        }

        ScheduledPost schedule = new ScheduledPost();
        schedule.setChatId(chatId);
        schedule.setGroupName(groupName);
        schedule.setCronExpression(cronResult.getCronExpression());
        schedule.setDescription(cronResult.getDescription());
        schedule.setMessageText(messageText);
        schedule.setImageUrl(imageUrl);
        schedule.setCreatedBy(createdBy);
        schedule.setIsActive(true);

        ScheduledPost saved = scheduledPostRepository.save(schedule);
        log.info("Created schedule: {} for chat {} with cron: {}",
                groupName, chatId, cronResult.getCronExpression());
        return saved;
    }

    /**
     * Получаем все активные расписания
     */
    public List<ScheduledPost> getActiveSchedules() {
        return scheduledPostRepository.findByIsActiveTrue();
    }

    /**
     * Получаем активные расписания для конкретного чата
     */
    public List<ScheduledPost> getActiveSchedulesForChat(Long chatId) {
        return scheduledPostRepository.findByChatIdAndIsActiveTrue(chatId);
    }

    /**
     * Проверяем, должно ли выполняться расписание в указанное время
     */
    public boolean shouldExecute(ScheduledPost schedule, LocalDateTime dateTime) {
        return cronService.shouldExecute(schedule.getCronExpression(), dateTime);
    }

    public List<ScheduledPost> getActiveSchedulesForGroup(Long chatId, String groupName) {
        return scheduledPostRepository.findByChatIdAndGroupNameAndIsActiveTrue(chatId, groupName);
    }

    @Transactional
    public void markAsSent(Long scheduleId) {
        ScheduledPost schedule = scheduledPostRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Расписание не найдено"));
        schedule.setLastSent(LocalDateTime.now());
        scheduledPostRepository.save(schedule);
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

    public Optional<ScheduledPost> findById(Long scheduleId) {
        return scheduledPostRepository.findById(scheduleId);
    }
}