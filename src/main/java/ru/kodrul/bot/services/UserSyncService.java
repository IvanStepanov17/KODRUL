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
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Создание или обновление пользователя по username
     */
    @Transactional
    public TelegramUser syncUserByUsername(String username, Long mentionedByUserId, Long chatId) {
        String cleanUsername = username.startsWith("@") ? username.substring(1) : username;

        Optional<TelegramUser> existingUser = userRepository.findByUserName(cleanUsername);

        if (existingUser.isPresent()) {
            TelegramUser user = existingUser.get();
            if (isTemporaryUserId(user.getUserId())) {
                log.debug("User @{} has temporary ID, keeping it until real activity", cleanUsername);
            }
            return user;
        }

        TelegramUser newUser = new TelegramUser();
        newUser.setUserId(generateTemporaryUserId());
        newUser.setUserName(cleanUsername);
        newUser.setFirstName(cleanUsername);
        newUser.setLastName("");
        newUser.setIsBot(false);
        newUser.setFirstSeen(java.time.LocalDateTime.now());
        newUser.setLastSeen(java.time.LocalDateTime.now());

        TelegramUser savedUser = userRepository.save(newUser);

        log.info("Created new user from username mention: @{} by user {} in chat {}",
                cleanUsername, mentionedByUserId, chatId);

        return savedUser;
    }

    /**
     * Обновление временного пользователя при получении реального user_id
     */
    @Transactional
    public TelegramUser updateTemporaryUser(String username, Long realUserId) {
        Optional<TelegramUser> userOpt = userRepository.findByUserName(username);

        if (userOpt.isPresent()) {
            TelegramUser user = userOpt.get();
            if (isTemporaryUserId(user.getUserId())) {
                log.info("Updating temporary user @{} with real ID: {}", username, realUserId);
                user.setUserId(realUserId);
                return userRepository.save(user);
            }
        }

        return null;
    }

    /**
     * Проверка, является ли ID временным
     */
    public boolean isTemporaryUserId(Long userId) {
        return userId != null && userId < 0;
    }

    private Long generateTemporaryUserId() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1_000_000_000);
        return -Math.abs(timestamp + random);
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
     * Очистка временных пользователей (тех что добавили командой /addusers, но они не проявили активность за последний месяц)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupTemporaryUsers() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<TelegramUser> users = userRepository.findOldNegativeUsersWithMembers(oneMonthAgo);
        log.info("Cleaned up temporary user: {}", users);
        userRepository.deleteAll(users);
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
            // TODO реализовать синхронизацию для планировщика. А нужна ли вообще ежедневная синхронизация всех пользователей?
        } catch (Exception e) {
            log.error("Unexpected error syncing users for chat {}", chatId, e);
        }
    }

    private boolean equalsSafe(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }
}