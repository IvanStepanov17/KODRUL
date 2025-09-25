package ru.kodrul.bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.kodrul.bot.abilities.GroupDistributeAbility;
import ru.kodrul.bot.abilities.HelperAbility;
import ru.kodrul.bot.abilities.RandomizeAbility;
import ru.kodrul.bot.abilities.RouletteAbility;
import ru.kodrul.bot.abilities.admin.ChatMemberAbility;
import ru.kodrul.bot.abilities.admin.GroupManagementAbility;
import ru.kodrul.bot.abilities.admin.UserManagementAbility;
import ru.kodrul.bot.config.BotProperties;
import ru.kodrul.bot.handlers.ResponseHandler;
import ru.kodrul.bot.services.GroupDistributeService;
import ru.kodrul.bot.services.RandomizeService;
import ru.kodrul.bot.services.RouletteService;

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
            RouletteService rouletteService,
            GroupDistributeService groupDistributeService,
            RandomizeService randomizeService,
            @Lazy GroupManagementAbility groupManagementAbility,
            @Lazy ChatMemberAbility chatMemberAbility,
            @Lazy UserManagementAbility userManagementAbility
    ) {
        super(environment.getProperty("bot.token"), environment.getProperty("bot.name"));
        this.handlers = handlers;
        this.properties = properties;
        addExtensions(
                new RouletteAbility(this, rouletteService),
                new RandomizeAbility(this, randomizeService),
                new GroupDistributeAbility(this, groupDistributeService),
                new HelperAbility(this),
                groupManagementAbility,
                chatMemberAbility,
                userManagementAbility
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
