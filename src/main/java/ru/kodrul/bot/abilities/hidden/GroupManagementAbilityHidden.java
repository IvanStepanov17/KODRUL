package ru.kodrul.bot.abilities.hidden;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.common.CommonAbilityHelper;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.services.AuthorizationService;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.MemberManagementService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupManagementAbilityHidden implements AbilityExtension {

    private final GroupManagementService groupManagementService;
    private final AuthorizationService authorizationService;
    private final SendService sendService;
    private final CommonAbilityHelper commonAbilityHelper;
    private final MemberManagementService memberManagementService;

    public Ability createGroupHiddenAbility() {
        return Ability.builder()
                .name("creategrouphidden")
                .locality(USER)
                .privacy(PUBLIC)
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

                                        Пример: /creategrouphidden -100123456789 НазваниеГруппы Скрытно созданная группа
                                        """
                        );
                        return;
                    }

                    try {
                        Long targetChatId = Long.parseLong(args[0]);
                        String groupName = args[1];
                        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

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
                                        🆔 *ID группы:* %d
                                """,
                                groupName,
                                chatTitle,
                                targetChatId,
                                "📝 *Описание:* " + description + "\n",
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

    public Ability addMembersAbility() {
        return Ability.builder()
                .name("addmembershidden")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> {

                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        log.warn("Unauthorized access attempt by user: {}", userId);
                        return;
                    }

                    memberManagementService.handleMemberOperation(ctx, true, true);
                })
                .build();
    }

    public Ability removeMembersAbility() {
        return Ability.builder()
                .name("removemembershidden")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> {

                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        log.warn("Unauthorized access attempt by user: {}", userId);
                        return;
                    }

                    memberManagementService.handleMemberOperation(ctx, false, true);
                })
                .build();
    }

    public Ability listGroupsAbility() {
        return Ability.builder()
                .name("listgroupshidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {

                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        log.warn("Unauthorized access attempt by user: {}", userId);
                        return;
                    }

                    String[] args = ctx.arguments();

                    if (args.length < 1) {
                        sendService.sendToUser(userId,
                                """
                                        Использование: /listgroupshidden <chat_id>
                                        
                                        Пример: /listgroupshidden -100123456789
                                        """
                        );
                        return;
                    }

                    Long targetChatId = Long.parseLong(args[0]);
                    List<ChatGroup> groups = groupManagementService.getChatGroups(targetChatId);
                    if (groups.isEmpty()) {
                        sendService.sendMessageToThread(ctx, "В этом чате еще нет групп");
                        return;
                    }

                    StringBuilder response = new StringBuilder("📋 Группы в этом чате:\n\n");
                    groups.forEach(group ->
                            response.append(groupManagementService.formatGroupInfo(group)).append("\n\n")
                    );

                    sendService.sendMessageToThread(ctx, response.toString());
                })
                .build();
    }

    public Ability groupInfoAbility() {
        return Ability.builder()
                .name("groupinfohidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(2)
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
                                        Использование: /groupinfohidden <chat_id> <название группы>
                                        
                                        Пример: /groupinfohidden -100123456789 Тест
                                        """
                        );
                        return;
                    }

                    try {
                        Long targetChatId = Long.parseLong(args[0]);
                        String groupName = args[1];
                        Optional<ChatGroup> groupOpt = groupManagementService.getGroupByNameWithMembersAndUsers(targetChatId, groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            StringBuilder response = new StringBuilder();
                            response.append("📊 *Детальная информация о группе*\n\n");
                            response.append(groupManagementService.formatGroupInfoWithMembers(group)).append("\n\n");

                            if (group.getMembers() == null || group.getMembers().isEmpty()) {
                                response.append("👥 *Участники:* группа пуста\n");
                            } else {
                                response.append("👥 *Участники (").append(group.getMembers().size()).append("):*\n");

                                for (int i = 0; i < group.getMembers().size(); i++) {
                                    GroupMember member = group.getMembers().get(i);
                                    String userInfo = groupManagementService.formatUserInfoForGroup(member);
                                    response.append(i + 1).append(". ").append(userInfo).append("\n");
                                }
                            }

                            sendService.sendMessageToThread(ctx, response.toString(), Constants.PARSE_MARKDOWN);
                        } else {
                            sendService.sendMessageToThread(ctx, "❌ Группа '" + groupName + "' не найдена в этом чате");
                        }
                    } catch (Exception e) {
                        log.error("Error getting group info", e);
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при получении информации о группе: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability addTrustedUserAbility() {
        return Ability.builder()
                .name("addtrusteduser")
                .locality(USER)
                .privacy(PUBLIC)
                .input(2)
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
                                        Использование: /addtrusteduser <user_id> <admin_key>

                                        Пример: /addtrusteduser 123456789 секретный-ключ
                                        """
                        );
                        return;
                    }

                    try {
                        Long newUserId = Long.parseLong(args[0]);
                        String adminKey = args[1];

                        authorizationService.addTrustedUser(newUserId, adminKey);

                        String message = String.format(
                                """
                                        ✅ *Пользователь добавлен в доверенные!*

                                        👤 *User ID:* %d
                                        📊 *Всего доверенных:* %d
                                """,
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