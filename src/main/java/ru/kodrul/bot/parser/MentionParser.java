package ru.kodrul.bot.parser;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.kodrul.bot.utils.Helper;

import java.util.ArrayList;
import java.util.List;

@Component
public class MentionParser {

    /**
     * Парсит упоминания из сообщения
     */
    public List<ParsedMention> parseMentions(String messageText, List<MessageEntity> entities) {
        List<ParsedMention> mentions = new ArrayList<>();

        if (entities == null || messageText == null) {
            return mentions;
        }

        for (MessageEntity entity : entities) {
            if ("mention".equals(entity.getType()) || "text_mention".equals(entity.getType())) {
                String mentionText = messageText.substring(entity.getOffset(),
                        entity.getOffset() + entity.getLength());

                if ("text_mention".equals(entity.getType()) && entity.getUser() != null) {
                    // Упоминание с user_id
                    mentions.add(new ParsedMention(
                            entity.getUser().getId(),
                            Helper.escapeMarkdownV2(entity.getUser().getUserName()),
                            mentionText
                    ));
                } else if ("mention".equals(entity.getType())) {
                    // Упоминание по username (@username)
                    String username = mentionText.startsWith("@") ?
                            mentionText.substring(1) : mentionText;
                    mentions.add(new ParsedMention(null, Helper.escapeMarkdownV2(username), mentionText));
                }
            }
        }

        return mentions;
    }

    /**
     * Находит упоминания, соответствующие определенным пользователям
     */
    public List<ParsedMention> findMentionsForUsers(List<ParsedMention> mentions, List<Long> userIds) {
        List<ParsedMention> result = new ArrayList<>();
        for (ParsedMention mention : mentions) {
            if (mention.getUserId() != null && userIds.contains(mention.getUserId())) {
                result.add(mention);
            }
        }
        return result;
    }
}