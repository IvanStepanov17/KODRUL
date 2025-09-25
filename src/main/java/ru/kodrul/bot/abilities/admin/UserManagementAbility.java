package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.TelegramUser;
import ru.kodrul.bot.parser.MentionParser;
import ru.kodrul.bot.parser.ParsedMention;
import ru.kodrul.bot.services.UserSyncService;

import java.util.List;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;

@Component
@RequiredArgsConstructor
public class UserManagementAbility implements AbilityExtension {

    @Lazy
    private final AbilityBot abilityBot;
    private final UserSyncService userSyncService;
    private final MentionParser mentionParser;

    /**
     * Команда для ручного добавления пользователей по username
     */
    public Ability addUsersAbility() {
        return Ability.builder()
                .name("addusers")
                .info("Добавить нескольких пользователей по username")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> {
                    try {
                        List<ParsedMention> mentions = mentionParser.parseMentions(
                                ctx.update().getMessage().getText(),
                                ctx.update().getMessage().getEntities()
                        );

                        if (mentions.isEmpty()) {
                            abilityBot.silent().send("❌ Не найдено упоминаний пользователей", ctx.chatId());
                            return;
                        }

                        int addedCount = 0;
                        int existingCount = 0;

                        for (ParsedMention mention : mentions) {
                            if (mention.getUsername() != null) {
                                TelegramUser user = userSyncService.syncUserByUsername(
                                        mention.getUsername(), ctx.user().getId(), ctx.chatId()
                                );

                                if (userSyncService.isTemporaryUserId(user.getUserId())) {
                                    addedCount++;
                                } else {
                                    existingCount++;
                                }
                            }
                        }

                        String response = String.format(
                                "✅ Обработано упоминаний: %d\n" +
                                        "👥 Новых пользователей: %d\n" +
                                        "💾 Уже в базе: %d",
                                mentions.size(), addedCount, existingCount
                        );

                        abilityBot.silent().send(response, ctx.chatId());

                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при добавлении пользователей: " + e.getMessage(), ctx.chatId());
                    }
                })
                .build();
    }
}