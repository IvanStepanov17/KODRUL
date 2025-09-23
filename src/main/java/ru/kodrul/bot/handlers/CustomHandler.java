package ru.kodrul.bot.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class CustomHandler extends ResponseHandler {

    @Override
    public boolean canAccept(Update update) {
        return update.getMessage() != null && update.getMessage().getText() != null;
    }

    @Override
    public void handle(Update update, SilentSender sender) {
        System.out.println("CustomHandler: " + update.getMessage().getText());
    }
}
