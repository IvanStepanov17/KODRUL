package ru.kodrul.bot.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.kodrul.bot.pojo.CommandArguments;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CommandParser {

    private static final Pattern SCHEDULE_HIDDEN_PATTERN = Pattern.compile(
      "^/\\w+\\s+" +                   // команда
            "(-?\\d+)\\s+" +                 // chat_id (число, может быть отрицательным)
            "(?:(\\d+)\\s+)?" +              // message_thread_id (опционально, число)
            "(\\S+)\\s+" +                   // group_name (без пробелов)
            "[\"']([^\"']+)[\"']\\s+" +      // schedule в кавычках
            "[\"']([^\"']+)[\"']" +          // message в кавычках
            "(?:\\s+(https?://[^\\s]+))?" +  // image_url (опционально)
            "$"
    );

    private static final Pattern SCHEDULE_PATTERN = Pattern.compile(
            "^/\\w+\\s+" +                   // команда
            "(\\S+)\\s+" +                   // group_name (без пробелов)
            "[\"']([^\"']+)[\"']\\s+" +      // schedule в кавычках
            "([\\s\\S]*)$"                   // остальное сообщение (может содержать пробелы и URL)
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(https?://[^\\s]+)$"         // URL в конце сообщения
    );

    /**
     * Парсит команду с кавычками для скрытых расписания с учётом chat_id
     */
    public CommandArguments parseCommandWithChatIdAndQuotes(String fullText) {
        CommandArguments args = new CommandArguments();

        try {
            Matcher matcher = SCHEDULE_HIDDEN_PATTERN.matcher(fullText);
            if (!matcher.find()) {
                log.warn("Failed to parse command: {}", fullText);
                return args;
            }

            args.setChatId(Long.parseLong(matcher.group(1)));

            String threadIdStr = matcher.group(2);
            if (threadIdStr != null && !threadIdStr.isEmpty()) {
                args.setThreadId(Integer.parseInt(threadIdStr));
            }

            args.setGroupName(matcher.group(3));
            args.setSchedule(matcher.group(4));
            args.setMessage(matcher.group(5));
            args.setImageUrl(matcher.group(6));

            log.debug("Parsed command: chatId={}, threadId={}, group={}, schedule='{}', message='{}', imageUrl={}",
                    args.getChatId(), args.getThreadId(), args.getGroupName(), args.getSchedule(),
                    args.getMessage(), args.getImageUrl());

        } catch (Exception e) {
            log.error("Error parsing command: {}", fullText, e);
        }

        return args;
    }

    /**
     * Парсит команду с кавычками для расписания
     */
    public CommandArguments parseCommandWithQuotes(String fullText) {
        CommandArguments args = new CommandArguments();

        try {
            Matcher matcher = SCHEDULE_PATTERN.matcher(fullText);
            if (!matcher.find()) {
                log.warn("Failed to parse command: {}", fullText);
                return args;
            }

            args.setGroupName(matcher.group(1));
            args.setSchedule(matcher.group(2));

            String messagePart = matcher.group(3).trim();

            // Проверяем, есть ли URL в конце сообщения
            Matcher urlMatcher = URL_PATTERN.matcher(messagePart);
            if (urlMatcher.find()) {
                args.setImageUrl(urlMatcher.group(1));
                args.setMessage(messagePart.substring(0, urlMatcher.start()).trim());
            } else {
                args.setMessage(messagePart);
            }

            log.debug("Parsed command: group={}, schedule='{}', message='{}', imageUrl={}",
                    args.getGroupName(), args.getSchedule(),
                    args.getMessage(), args.getImageUrl());

        } catch (Exception e) {
            log.error("Error parsing command: {}", fullText, e);
        }

        return args;
    }

    private char findQuoteChar(String text) {
        int singleQuote = text.indexOf('\'');
        int doubleQuote = text.indexOf('"');

        if (singleQuote == -1 && doubleQuote == -1) return 0;
        if (singleQuote == -1) return '"';
        if (doubleQuote == -1) return '\'';

        // Возвращаем ту кавычку, которая встречается раньше
        return singleQuote < doubleQuote ? '\'' : '"';
    }
}