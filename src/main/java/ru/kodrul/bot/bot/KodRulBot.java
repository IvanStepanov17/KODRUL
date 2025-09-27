package ru.kodrul.bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.kodrul.bot.abilities.RandomizeAbility;
import ru.kodrul.bot.abilities.RouletteAbility;
import ru.kodrul.bot.abilities.admin.ChatMemberAbility;
import ru.kodrul.bot.abilities.admin.GroupManagementAbility;
import ru.kodrul.bot.abilities.admin.ScheduleAbility;
import ru.kodrul.bot.abilities.admin.UserManagementAbility;
import ru.kodrul.bot.config.BotProperties;
import ru.kodrul.bot.handlers.ResponseHandler;

import java.util.Set;

@Slf4j
@Service
public class KodRulBot extends AbilityBot {

    private final Set<ResponseHandler> handlers;
    private final BotProperties properties;

    public KodRulBot(
            Environment environment,
            Set<ResponseHandler> handlers,
            BotProperties properties,
            @Lazy RouletteAbility rouletteAbility,
            @Lazy RandomizeAbility randomizeAbility,
            @Lazy GroupManagementAbility groupManagementAbility,
            @Lazy ChatMemberAbility chatMemberAbility,
            @Lazy UserManagementAbility userManagementAbility,
            @Lazy ScheduleAbility scheduleAbility
    ) {
        super(environment.getProperty("bot.token"), environment.getProperty("bot.name"));
        this.handlers = handlers;
        this.properties = properties;
        addExtensions(
                rouletteAbility,
                randomizeAbility,
                groupManagementAbility,
                chatMemberAbility,
                userManagementAbility,
                scheduleAbility
        );
    }

    @Override
    public void onUpdateReceived(Update update) {
        handlers.stream()
                .filter(handler -> handler.canAccept(update))
                .forEach(handler -> {
                    try {
                        handler.handle(update, silent);
                    } catch (Exception e) {
                        log.error("Error in handler {}", handler.getClass().getSimpleName(), e);
                    }
                });
        log.info("Receive new Update. updateID: {}", update.getUpdateId());
        super.onUpdateReceived(update);
    }

    @Override
    public long creatorId() {
        return properties.getBotCreator();
    }
}
