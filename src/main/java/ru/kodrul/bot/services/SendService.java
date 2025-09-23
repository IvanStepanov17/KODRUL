package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.kodrul.bot.bot.KodRulBot;

@Service
@RequiredArgsConstructor
public class SendService {

    private final KodRulBot bot;

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
//            log.error("Ошибка отправки сообщения");
            throw new RuntimeException(e);
        }
    }
}
