package ru.kodrul.bot.exceptions;

public class GroupAlreadyExistsException extends RuntimeException {

    public GroupAlreadyExistsException(String groupName, Long chatId) {
        super(String.format("Группа '%s' уже существует в чате %d", groupName, chatId));
    }
}
