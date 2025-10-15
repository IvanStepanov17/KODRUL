package ru.kodrul.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.kodrul.bot.exceptions.NotTrustedUserException;
import ru.kodrul.bot.services.AuthorizationService;

import java.util.Set;

// TODO проверка на доверенного пользователя вынесена в отдельный обработчик. Проверить работает ли и удалить ненужные проверки на доверенного из конкретных команд.
@Slf4j
@Component
@RequiredArgsConstructor
public class HiddenCommandHandler extends ResponseHandler {

    private final AuthorizationService authorizationService;

    private final Set<String> HIDDEN_COMMANDS = Set.of(
            "creategrouphidden",
            "addtrusteduser",
            "listtrustedusers",
            "createschedulehidden",
            "listscheduleshidden",
            "toggleschedulehidden",
            "deleteschedulehidden",
            "scheduleinfohidden",
            "helphidden"
    );

    @Override
    public boolean canAccept(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            return HIDDEN_COMMANDS.stream().anyMatch(cmd -> text.startsWith("/" + cmd));
        }
        return false;
    }

    @Override
    public void handle(Update update, SilentSender sender) {
        Long userId = update.getMessage().getFrom().getId();
        String command = update.getMessage().getText().split(" ")[0].substring(1);

        if (!authorizationService.isTrustedUser(userId)) {
            log.warn("Unauthorized attempt to use hidden command /{} by user {}", command, userId);
            throw new NotTrustedUserException(command, userId);
        }
    }
}