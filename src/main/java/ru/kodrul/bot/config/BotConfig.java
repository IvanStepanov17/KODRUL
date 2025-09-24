package ru.kodrul.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.abilitybots.api.bot.AbilityBot;
import ru.kodrul.bot.handlers.MemberOperationHandler;
import ru.kodrul.bot.parser.MentionParser;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.UserSyncService;

@Configuration
public class BotConfig {

    @Bean
    public MentionParser mentionParser() {
        return new MentionParser();
    }

    @Bean
    public MemberOperationHandler memberOperationHandler(
            GroupManagementService groupService,
            UserSyncService userSyncService,
            MentionParser mentionParser,
            AbilityBot abilityBot) {
        return new MemberOperationHandler(groupService, userSyncService, mentionParser, abilityBot.silent());
    }
}