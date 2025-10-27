package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.TelegramUser;
import ru.kodrul.bot.parser.MentionParser;
import ru.kodrul.bot.pojo.ParsedMention;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.services.UserSyncService;

import java.util.List;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;

@Component
@RequiredArgsConstructor
public class UserManagementAbility implements AbilityExtension {

    private final UserSyncService userSyncService;
    private final MentionParser mentionParser;
    private final SendService sendService;

    /**
     * Команда для ручного добавления пользователей по username
     */
    public Ability addUsersAbility() {
        return Ability.builder()
                .name("addusers")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> {
                    try {
                        List<ParsedMention> mentions = mentionParser.parseMentions(
                                ctx.update().getMessage().getText(),
                                ctx.update().getMessage().getEntities()
                        );

                        if (mentions.isEmpty()) {
                            sendService.sendMessageToThread(ctx, "❌ Не найдено упоминаний пользователей");
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
                                """
                                        ✅ Обработано упоминаний: %d
                                        👥 Новых пользователей: %d
                                        💾 Уже в базе: %d""",
                                mentions.size(), addedCount, existingCount
                        );

                        sendService.sendMessageToThread(ctx, response);

                    } catch (Exception e) {
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при добавлении пользователей: " + e.getMessage());
                    }
                })
                .build();
    }
}