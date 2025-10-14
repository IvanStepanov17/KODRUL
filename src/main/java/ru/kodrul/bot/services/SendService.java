package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendService {

    @Lazy
    private final AbilityBot abilityBot;

    public void sendMessageToThread(MessageContext ctx, String text) {
        var chatId = ctx.chatId();
        var messageThreadId = ctx.update().getMessage().getMessageThreadId();
        send(chatId, messageThreadId, text, null);
    }

    public void sendMessageToThread(MessageContext ctx, String text, String parseMode) {
        var chatId = ctx.chatId();
        var messageThreadId = ctx.update().getMessage().getMessageThreadId();
        send(chatId, messageThreadId, text, parseMode);
    }

    /**
     * Отправка сообщения в конкретный топик супергруппы
     *
     * @param chatId id чата
     * @param messageThreadId id топика
     * @param text текст сообщения
     * @param parseMode режим парсинга ("Markdown", "MarkdownV2" или null (для обычного текста)
     */
    private void send(Long chatId, Integer messageThreadId, String text, String parseMode) {
        try {
            var message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);

            if (messageThreadId != null) {
                message.setMessageThreadId(messageThreadId);
            }

            if (parseMode != null) {
                message.setParseMode(parseMode);
            }

            abilityBot.execute(message);
            log.info("Message sent to chat: {}, thread: {}, parseMode: {}", chatId, messageThreadId, parseMode);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {} thread {}: {}",
                    chatId, messageThreadId, e.getMessage());
        }
    }
}