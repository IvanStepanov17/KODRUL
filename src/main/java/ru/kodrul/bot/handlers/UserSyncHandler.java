package ru.kodrul.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.kodrul.bot.entity.TelegramUser;
import ru.kodrul.bot.parser.MentionParser;
import ru.kodrul.bot.pojo.ParsedMention;
import ru.kodrul.bot.services.UserSyncService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncHandler extends ResponseHandler {

    private final UserSyncService userSyncService;
    private final MentionParser mentionParser;

    /**
    * Обрабатываем новые сообщения от пользователей или новых участников чата
    */
    @Override
    public boolean canAccept(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            return (message.getFrom() != null && !message.getFrom().getIsBot()) ||
                    (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) ||
                    hasMentions(message);
        }
        return false;
    }

    @Override
    public void handle(Update update, SilentSender sender) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        // Обрабатываем упоминания в сообщении
        if (hasMentions(message)) {
            handleMentions(message, chatId);
        }

        // Обрабатываем новых участников чата
        if (message.getNewChatMembers() != null) {
            handleNewChatMembers(message, chatId);
        }

        // Обрабатываем отправителя сообщения (если это не бот)
        if (message.getFrom() != null && !message.getFrom().getIsBot()) {
            handleMessageSender(message.getFrom(), chatId);
        }
    }

    /**
     * Обработка упоминаний в сообщении
     */
    private void handleMentions(Message message, Long chatId) {
        List<ParsedMention> mentions = mentionParser.parseMentions(
                message.getText(),
                message.getEntities()
        );

        for (ParsedMention mention : mentions) {
            try {
                if (mention.getUserId() != null) {
                    // Упоминание с user_id - синхронизируем
                    userSyncService.syncUserWithChat(mention.getUserId(), chatId);
                } else if (mention.getUsername() != null) {
                    // Упоминание по username - ищем в базе или через API
                    syncUserByUsername(mention.getUsername(), chatId);
                }
            } catch (Exception e) {
                log.warn("Failed to sync mentioned user: {}", mention.getText(), e);
            }
        }
    }

    private void syncUserByUsername(String username, Long chatId) {
        Optional<TelegramUser> userOpt = userSyncService.findUserByUsername(username);
        if (userOpt.isPresent()) {
            return;
        }

        // TODO Если нет в базе, пытаемся получить через API
        log.debug("User @{} mentioned but not found in database", username);
    }

    private boolean hasMentions(Message message) {
        return message.getEntities() != null &&
                message.getEntities().stream()
                        .anyMatch(entity -> "mention".equals(entity.getType()) ||
                                "text_mention".equals(entity.getType()));
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
            Optional<TelegramUser> existingUser = userSyncService.findUserByUsername(user.getUserName());

            if (existingUser.isPresent() &&
                    userSyncService.isTemporaryUserId(existingUser.get().getUserId())) {

                TelegramUser updatedUser = userSyncService.updateTemporaryUser(
                        user.getUserName(), user.getId()
                );

                if (updatedUser != null) {
                    log.info("Updated temporary user @{} with real ID: {}",
                            user.getUserName(), user.getId());
                }
            }

            userSyncService.syncUser(user);

        } catch (Exception e) {
            log.error("Failed to sync message sender {} in chat {}", user.getId(), chatId, e);
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