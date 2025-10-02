package ru.kodrul.bot.exceptions;

public class UserAlreadyInGroupException extends RuntimeException
{
    public UserAlreadyInGroupException(Long userId, Long groupId) {
        super(String.format("Пользователь %d уже находится в группе %d", userId, groupId));
    }
}