package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.entity.ScheduledPost;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledMessageService {

    private final ScheduledService scheduledService;
    private final GroupManagementService groupManagementService;
    private final AbilityBot abilityBot;

    /**
     * Проверяем расписания каждую минуту
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkScheduledPosts() {
        LocalDateTime currentDateTime = LocalDateTime.now().withSecond(0).withNano(0);
        List<ScheduledPost> activeSchedules = scheduledService.getActiveSchedules();

        log.debug("Checking {} active schedules for time: {}", activeSchedules.size(), currentDateTime);

        for (ScheduledPost schedule : activeSchedules) {
            try {
                if (scheduledService.shouldExecute(schedule, currentDateTime)) {
                    sendScheduledMessage(schedule);
                    scheduledService.markAsSent(schedule.getId());
                    log.info("Executed schedule: {} for group {}", schedule.getId(), schedule.getGroupName());
                }
            } catch (Exception e) {
                log.error("Failed to process schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }

    private void sendScheduledMessage(ScheduledPost schedule) {
        try {
            Optional<ChatGroup> groupOpt = groupManagementService.getGroupByNameWithMembersAndUsers(
                    schedule.getChatId(), schedule.getGroupName());

            if (groupOpt.isEmpty()) {
                log.warn("Group {} not found for chat {}", schedule.getGroupName(), schedule.getChatId());
                return;
            }

            ChatGroup group = groupOpt.get();
            List<GroupMember> members = group.getMembers();

            if (members.isEmpty()) {
                log.warn("Group {} is empty, skipping scheduled post", schedule.getGroupName());
                return;
            }

            StringBuilder message = new StringBuilder();
            for (GroupMember member : members) {
                String username = member.getUser().getUserName();
                if (username != null && !username.isEmpty()) {
                    message.append("@").append(username).append(" ");
                }
            }

            if (schedule.getMessageText() != null && !schedule.getMessageText().isEmpty()) {
                message.append("\n\n").append(schedule.getMessageText());
            }

            String finalMessage = message.toString();

            if (finalMessage.length() > 4000) {
                finalMessage = finalMessage.substring(0, 4000) + "...";
                log.warn("Message truncated for schedule {}", schedule.getId());
            }

            if (schedule.getImageUrl() != null && !schedule.getImageUrl().isEmpty()) {
                sendPhotoMessage(schedule.getChatId(), schedule.getMessageThreadId(), schedule.getImageUrl(), finalMessage);
            } else {
                sendTextMessage(schedule.getChatId(), schedule.getMessageThreadId(), finalMessage);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error sending scheduled message: " + e.getMessage(), e);
        }
    }

    private void sendTextMessage(Long chatId, Integer messageThreadId, String text) throws TelegramApiException {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);

            if (messageThreadId != null) {
                message.setMessageThreadId(messageThreadId);
                log.debug("Sending text message to thread {} in chat {}", messageThreadId, chatId);
            }

            abilityBot.execute(message);
        } catch (Exception e) {
            log.error("Failed to send text message to chat {} thread {}: {}",
                    chatId, messageThreadId, e.getMessage());
            throw e;
        }
    }

    private void sendPhotoMessage(Long chatId, Integer messageThreadId, String imageUrl, String caption) throws TelegramApiException {
        try {
            InputFile photo = new InputFile(imageUrl);
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(photo);

            if (caption.length() > 1024) {
                caption = caption.substring(0, 1020) + "...";
            }
            sendPhoto.setCaption(caption);

            if (messageThreadId != null) {
                sendPhoto.setMessageThreadId(messageThreadId);
                log.debug("Sending photo message to thread {} in chat {}", messageThreadId, chatId);
            }

            abilityBot.execute(sendPhoto);

        } catch (TelegramApiException e) {
            log.error("Failed to send photo message to chat {} thread {}: {}",
                    chatId, messageThreadId, e.getMessage());

            String fallbackMessage = caption + "\n\n🖼️ Изображение: " + imageUrl;
            sendTextMessage(chatId, messageThreadId, fallbackMessage);
        } catch (Exception e) {
            log.error("Unexpected error sending photo to chat {} thread {}: {}",
                    chatId, messageThreadId, e.getMessage());
            throw e;
        }
    }
}