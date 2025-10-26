package ru.kodrul.bot.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedMention {

    private final Long userId;
    private final String username;
    private final String text;
}