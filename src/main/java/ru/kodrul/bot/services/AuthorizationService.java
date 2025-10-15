package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.kodrul.bot.config.TrustedUsersConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final TrustedUsersConfig trustedUsersConfig;

    /**
     * Проверяет, является ли пользователь доверенным
     */
    public boolean isTrustedUser(Long userId) {
        boolean isTrusted = trustedUsersConfig.getUserIds().contains(userId);
        log.debug("User {} trusted: {}", userId, isTrusted);
        return isTrusted;
    }

    /**
     * Проверяет административный ключ
     */
    public boolean isValidAdminKey(String key) {
        return trustedUsersConfig.getAdminKey().equals(key);
    }

    /**
     * Добавляет нового доверенного пользователя (только для админов)
     */
    public synchronized void addTrustedUser(Long userId, String adminKey) {
        if (!isValidAdminKey(adminKey)) {
            throw new SecurityException("incorrect admin key");
        }

        if (!trustedUsersConfig.getUserIds().contains(userId)) {
            trustedUsersConfig.getUserIds().add(userId);
            log.info("Added trusted user: {}", userId);
        }
    }

    /**
     * Возвращает количество доверенных пользователей
     */
    public int getTrustedUsersCount() {
        return trustedUsersConfig.getUserIds().size();
    }
}