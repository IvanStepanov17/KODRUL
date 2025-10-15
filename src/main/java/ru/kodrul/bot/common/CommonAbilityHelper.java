package ru.kodrul.bot.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonAbilityHelper {

    @Lazy
    private final AbilityBot abilityBot;

    /**
     * Проверяет, является ли бот участником чата
     */
    public boolean isBotMemberOfChat(Long chatId) {
        try {
            GetChat getChat = new GetChat();
            getChat.setChatId(chatId.toString());
            abilityBot.execute(getChat);
            return true;
        } catch (TelegramApiException e) {
            log.debug("Bot is not member of chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Получает название чата
     */
    public String getChatTitle(Long chatId) {
        try {
            GetChat getChat = new GetChat();
            getChat.setChatId(chatId.toString());
            Chat chat = abilityBot.execute(getChat);
            return chat.getTitle() != null ? chat.getTitle() : "Unknown Chat";
        } catch (TelegramApiException e) {
            log.warn("Failed to get chat title for {}: {}", chatId, e.getMessage());
            return "Unknown Chat";
        }
    }

    /**
     * Возвращает количество участников чата
     */
    public Optional<Integer> getMemberCount(MessageContext ctx) {
        String chatId = ctx.chatId().toString();
        GetChatMemberCount countRequest = new GetChatMemberCount();
        countRequest.setChatId(chatId);
        return abilityBot.silent().execute(countRequest);
    }
}
