package ru.kodrul.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.kodrul.bot.services.UserSyncService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncHandler extends ResponseHandler {

    private final UserSyncService userSyncService;

    /**
    * Обрабатываем новые сообщения от пользователей или новых участников чата
    * */
    @Override
    public boolean canAccept(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            return (message.getFrom() != null && !message.getFrom().getIsBot()) ||
                    (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty());
        }
        return false;
    }

    @Override
    public void handle(Update update, SilentSender sender) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        try {
            // Обрабатываем новых участников чата
            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                handleNewChatMembers(message, chatId);
            }

            // Обрабатываем отправителя сообщения (если это не бот)
            if (message.getFrom() != null && !message.getFrom().getIsBot()) {
                handleMessageSender(message.getFrom(), chatId);
            }

            log.debug("User sync handled for chat {}: {}", chatId,
                    message.getFrom() != null ? message.getFrom().getId() : "unknown");

        } catch (Exception e) {
            log.error("Error in UserSyncHandler for chat {}", chatId, e);
        }
    }

    /**
     * Обработка новых участников чата
     */
    private void handleNewChatMembers(Message message, Long chatId) {
        for (User newUser : message.getNewChatMembers()) {
            try {
                if (newUser.getIsBot()) {
                    log.debug("Skipping bot user: {}", newUser.getUserName());
                    continue;
                }

                userSyncService.syncUser(newUser);

                log.info("New user added to chat {}: {} (ID: {})",
                        chatId,
                        formatUserInfo(newUser),
                        newUser.getId());

            } catch (Exception e) {
                log.error("Failed to sync new chat member {} in chat {}",
                        newUser.getId(), chatId, e);
            }
        }
    }

    /**
     * Обработка отправителя сообщения
     */
    private void handleMessageSender(User user, Long chatId) {
        try {

            userSyncService.syncUser(user);

            log.debug("User activity registered: {} in chat {}",
                    formatUserInfo(user), chatId);

        } catch (Exception e) {
            log.error("Failed to sync message sender {} in chat {}",
                    user.getId(), chatId, e);
        }
    }

    /**
     * Форматирование информации о пользователе для логирования
     */
    private String formatUserInfo(User user) {
        StringBuilder sb = new StringBuilder();

        if (user.getUserName() != null) {
            sb.append("@").append(user.getUserName());
        }

        if (!sb.isEmpty()) sb.append(" ");

        sb.append(user.getFirstName());

        if (user.getLastName() != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getLastName());
        }

        if (sb.isEmpty()) {
            sb.append("Unknown User");
        }

        sb.append(" (ID: ").append(user.getId()).append(")");
        return sb.toString();
    }
}