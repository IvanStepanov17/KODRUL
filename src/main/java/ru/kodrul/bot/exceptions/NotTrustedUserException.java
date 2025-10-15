package ru.kodrul.bot.exceptions;

public class NotTrustedUserException extends RuntimeException {

    public NotTrustedUserException(String commandName, Long userId) {
        super(String.format("Попытка использовать команду %s доступную только для доверенного пользователя. userId: %s",
                commandName,
                userId)
        );
    }
}
