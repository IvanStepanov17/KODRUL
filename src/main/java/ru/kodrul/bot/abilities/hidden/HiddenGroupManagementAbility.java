package ru.kodrul.bot.abilities.hidden;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.common.CommonAbilityHelper;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.services.AuthorizationService;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class HiddenGroupManagementAbility implements AbilityExtension {

    private final GroupManagementService groupManagementService;
    private final AuthorizationService authorizationService;
    private final SendService sendService;
    private final CommonAbilityHelper commonAbilityHelper;

    public Ability createGroupHiddenAbility() {
        return Ability.builder()
                .name("creategrouphidden")
//                .info("Создать группу для указанного чата (только для доверенных пользователей)")
                .locality(USER)
                .privacy(PUBLIC)
                .input(3)
                .action(ctx -> {

                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        log.warn("Unauthorized access attempt by user: {}", userId);
                        return;
                    }

                    String[] args = ctx.arguments();
                    if (args.length < 2) {
                        sendService.sendToUser(userId,
                                """
                                        Использование: /creategrouphidden <chat_id> <название_группы> [описание]

                                        Пример: /creategrouphidden -100123456789 НазваниеГруппы "Скрытно созданная группа\"""");
                        return;
                    }

                    try {
                        Long targetChatId = Long.parseLong(args[0]);
                        String groupName = args[1];
                        String description = args.length > 2 ? args[2] : null;

                        if (!commonAbilityHelper.isBotMemberOfChat(targetChatId)) {
                            sendService.sendToUser(userId,
                                    "❌ Бот не является участником указанного чата или чат не существует\n" +
                                            "Chat ID: " + targetChatId);
                            return;
                        }

                        String chatTitle = commonAbilityHelper.getChatTitle(targetChatId);

                        ChatGroup group = groupManagementService.createGroup(
                                groupName, description, targetChatId, chatTitle, userId
                        );

                        String successMessage = String.format(
                                """
                                        ✅ *Группа создана!*

                                        📋 *Группа:* %s
                                        💬 *Чат:* %s (ID: %d)
                                        %s\
                                        👤 *Создана:* %s (ID: %d)
                                        🆔 *ID группы:* %d""",
                                groupName,
                                chatTitle,
                                targetChatId,
                                description != null ? "📝 *Описание:* " + description + "\n" : "",
                                ctx.user().getFirstName(),
                                userId,
                                group.getId()
                        );

                        sendService.sendToUser(userId, successMessage, Constants.PARSE_MARKDOWN);
                        log.info("Group '{}' created by user {} for chat {}",
                                groupName, userId, targetChatId);

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ Chat ID должен быть числом");
                    } catch (IllegalArgumentException e) {
                        sendService.sendToUser(userId, "❌ " + e.getMessage());
                    } catch (Exception e) {
                        log.error("Error creating group for user {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка при создании группы: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability addTrustedUserAbility() {
        return Ability.builder()
                .name("addtrusteduser")
//                .info("Добавить доверенного пользователя (только с административным ключом)")
                .locality(USER)
                .privacy(PUBLIC)
                .input(2)
                .action(ctx -> {
                    Long userId = ctx.user().getId();
                    String[] args = ctx.arguments();

                    if (args.length < 2) {
                        sendService.sendToUser(userId,
                                "Использование: /addtrusteduser <user_id> <admin_key>\n\n" +
                                        "Пример: /addtrusteduser 123456789 секретный-ключ");
                        return;
                    }

                    try {
                        Long newUserId = Long.parseLong(args[0]);
                        String adminKey = args[1];

                        authorizationService.addTrustedUser(newUserId, adminKey);

                        String message = String.format(
                                "✅ *Пользователь добавлен в доверенные!*\n\n" +
                                        "👤 *User ID:* %d\n" +
                                        "📊 *Всего доверенных:* %d",
                                newUserId,
                                authorizationService.getTrustedUsersCount()
                        );

                        sendService.sendToUser(userId, message, Constants.PARSE_MARKDOWN);
                        log.info("User {} added trusted user: {}", userId, newUserId);

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ User ID должен быть числом");
                    } catch (SecurityException e) {
                        sendService.sendToUser(userId, "❌ " + e.getMessage());
                    } catch (Exception e) {
                        log.error("Error adding trusted user by {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability listTrustedUsersAbility() {
        return Ability.builder()
                .name("listtrustedusers")
//                .info("Показать количество доверенных пользователей")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> {
                    Long userId = ctx.user().getId();

                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        return;
                    }

                    int count = authorizationService.getTrustedUsersCount();
                    sendService.sendToUser(userId,
                            "📊 *Статистика доверенных пользователей:*\n\n" +
                                    "👥 *Количество:* " + count + "\n\n" +
                                    "💡 Для добавления используйте /addtrusteduser");
                })
                .build();
    }
}