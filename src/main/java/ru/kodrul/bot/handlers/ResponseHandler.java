package ru.kodrul.bot.handlers;

import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class ResponseHandler {

    public abstract boolean canAccept(Update update);

    public abstract void handle(Update update, SilentSender sender);

}
