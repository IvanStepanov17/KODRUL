package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.kodrul.bot.entity.TelegramUser;
import ru.kodrul.bot.repository.TelegramUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final TelegramUserRepository userRepository;

    /**
     * Обновляем данные, если они изменились
     * */
    @Transactional
    public TelegramUser syncUser(User telegramUser) {
        Optional<TelegramUser> existingUser = userRepository.findByUserId(telegramUser.getId());
        TelegramUser user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!equalsSafe(user.getUserName(), telegramUser.getUserName())) {
                user.setUserName(telegramUser.getUserName());
            }
            if (!equalsSafe(user.getFirstName(), telegramUser.getFirstName())) {
                user.setFirstName(telegramUser.getFirstName());
            }
            if (!equalsSafe(user.getLastName(), telegramUser.getLastName())) {
                user.setLastName(telegramUser.getLastName());
            }
            if (user.getIsBot() == null || !user.getIsBot().equals(telegramUser.getIsBot())) {
                user.setIsBot(telegramUser.getIsBot());
            }
            user.setLastSeen(LocalDateTime.now());
            user = userRepository.save(user);
        } else {
            user = new TelegramUser();
            user.setUserId(telegramUser.getId());
            user.setUserName(telegramUser.getUserName());
            user.setFirstName(telegramUser.getFirstName());
            user.setLastName(telegramUser.getLastName());
            user.setIsBot(telegramUser.getIsBot());
            user = userRepository.save(user);
            log.info("Created new user: {}", user);
        }

        return user;
    }

    @Transactional
    public TelegramUser syncUserWithChat(Long userId, Long chatId) {
        // TODO вызов Telegram API? Пока возвращаем существующего пользователя или создаем нового
        return userRepository.findByUserId(userId).orElseGet(() -> {
            TelegramUser newUser = new TelegramUser();
            newUser.setUserId(userId);
            newUser.setFirstName("Unknown");
            newUser.setLastName("User");
            return userRepository.save(newUser);
        });
    }

    public Optional<TelegramUser> findUserByUsername(String username) {
        return userRepository.findByUserName(username);
    }

    public Optional<TelegramUser> findUserById(Long userId) {
        return userRepository.findByUserId(userId);
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void scheduledUserSync() {
        log.info("Starting scheduled user synchronization");

        try {
            List<Long> activeChats = getChatsWhereBotIsMember();

            for (Long chatId : activeChats) {
                syncChatUsers(chatId);
            }

            log.info("Scheduled user synchronization completed. Processed {} chats", activeChats.size());
        } catch (Exception e) {
            log.error("Error during scheduled user synchronization", e);
        }
    }

    /**
     * Получаем чаты, где есть бот (из таблицы chat_groups)
     */
    private List<Long> getChatsWhereBotIsMember() {
        return userRepository.findDistinctChatIds();
    }

    /**
     * Синхронизация всех участников чата
     */
    @Transactional
    public void syncChatUsers(Long chatId) {
        try {
            // TODO реализовать синхронизацию
        } catch (Exception e) {
            log.error("Unexpected error syncing users for chat {}", chatId, e);
        }
    }

    /**
     * Синхронизация пользователей, которые уже есть в базе для этого чата
     */
    @Transactional
    public void syncExistingChatUsers(Long chatId) {
        // Здесь можно добавить логику для синхронизации пользователей,
        // которые уже были сохранены в базе для этого чата
        // Например, через группы или предыдущие сообщения

        log.debug("Syncing existing users for chat: {}", chatId);
    }

    private void createMinimalUser(Long userId) {
        if (!userRepository.findByUserId(userId).isPresent()) {
            TelegramUser user = new TelegramUser();
            user.setUserId(userId);
            user.setFirstName("Unknown");
            user.setLastName("User");
            userRepository.save(user);
            log.info("Created minimal user record for ID: {}", userId);
        }
    }

    private boolean equalsSafe(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }
}