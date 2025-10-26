package ru.kodrul.bot.pojo;

import lombok.Data;

@Data
public class CommandArguments {

    private Long chatId;
    private String groupName;
    private String schedule;
    private String message;
    private String imageUrl;
}