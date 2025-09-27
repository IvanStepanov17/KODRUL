package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemberService {

    private final AbilityBot abilityBot;

    /**
     * Получение информации о чате
     */
    public Chat getChat(Long chatId) throws TelegramApiException {
        GetChat getChat = new GetChat();
        getChat.setChatId(chatId.toString());
        return abilityBot.execute(getChat);
    }

    /**
     * Получение администраторов чата
     */
    public List<ChatMember> getChatAdministrators(Long chatId) throws TelegramApiException {
        GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
        getChatAdministrators.setChatId(chatId.toString());
        return abilityBot.execute(getChatAdministrators);
    }

    /**
     * Получение информации о конкретном участнике чата
     */
    public ChatMember getChatMember(Long chatId, Long userId) throws TelegramApiException {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(chatId.toString());
        getChatMember.setUserId(userId);
        return abilityBot.execute(getChatMember);
    }

    /**
     * Получение количества участников чата
     */
    public Integer getChatMemberCount(Long chatId) throws TelegramApiException {
        GetChatMemberCount countRequest = new GetChatMemberCount();
        countRequest.setChatId(chatId.toString());
        return abilityBot.execute(countRequest);
    }

    /**
     * Telegram Bot API не позволяет получить полный список участников чата
     * Можно получить только администраторов и информацию о конкретных пользователях
     */
    public List<ChatMember> getAvailableChatMembers(Long chatId) throws TelegramApiException {
        return getChatAdministrators(chatId);
    }
}