package ru.kodrul.bot.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CronParseResult {

    private final String cronExpression;
    private final String description;
    private final boolean success;
}