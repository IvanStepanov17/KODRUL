package ru.kodrul.bot.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super(String.format("Пользователь %d не найден", userId));
    }
}