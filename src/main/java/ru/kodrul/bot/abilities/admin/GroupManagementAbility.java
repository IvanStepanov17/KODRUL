package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.handlers.MemberOperationHandler;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.utils.Helper;

import java.util.List;
import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupManagementAbility implements AbilityExtension {

    @Lazy
    private final AbilityBot abilityBot;
    private final GroupManagementService groupService;
    private final MemberOperationHandler memberOperationHandler;

    public Ability createGroupAbility() {
        return Ability.builder()
                .name("creategroup")
                .info("Создать новую группу участников")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(2)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        abilityBot.silent().send("Использование: /creategroup <название> [описание]", ctx.chatId());
                        return;
                    }

                    try {
                        String name = args[0];
                        String description = args.length > 1 ? args[1] : null;
                        String chatTitle = ctx.update().getMessage().getChat().getTitle();

                        ChatGroup group = groupService.createGroup(name, description, ctx.chatId(), chatTitle, ctx.user().getId());
                        abilityBot.silent().send(
                                "✅ Группа создана!\n" + groupService.formatGroupInfo(group),
                                ctx.chatId()
                        );
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка: " + e.getMessage(), ctx.chatId());
                    }
                })
                .build();
    }

    public Ability listGroupsAbility() {
        return Ability.builder()
                .name("listgroups")
                .info("Показать все группы в чате")
                .locality(GROUP)
                .privacy(PUBLIC)
                .action(ctx -> {
                    List<ChatGroup> groups = groupService.getChatGroups(ctx.chatId());
                    if (groups.isEmpty()) {
                        abilityBot.silent().send("В этом чате еще нет групп", ctx.chatId());
                        return;
                    }

                    StringBuilder response = new StringBuilder("📋 Группы в этом чате:\n\n");
                    groups.forEach(group ->
                            response.append(groupService.formatGroupInfo(group)).append("\n\n")
                    );

                    abilityBot.silent().send(response.toString(), ctx.chatId());
                })
                .build();
    }

    public Ability deleteGroupAbility() {
        return Ability.builder()
                .name("deletegroup")
                .info("Удалить группу участников")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        abilityBot.silent().send("Использование: /deletegroup <название_группы>", ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Optional<ChatGroup> groupOpt = groupService.getGroupByName(ctx.chatId(), groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            // Проверяем, является ли пользователь создателем группы или администратором чата
                            if (!group.getCreatedBy().equals(ctx.user().getId())) {
                                // TODO Можно добавить дополнительную проверку прав администратора чата
                                abilityBot.silent().send("❌ Вы можете удалять только группы, созданные вами", ctx.chatId());
                                return;
                            }

                            String groupInfo = groupService.formatGroupInfo(group);
                            groupService.deleteGroup(group.getId());

                            abilityBot.silent().send("🗑️ Группа успешно удалена:\n" + groupInfo, ctx.chatId());
                        } else {
                            abilityBot.silent().send("❌ Группа '" + groupName + "' не найдена в этом чате", ctx.chatId());
                        }
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при удалении группы: " + e.getMessage(), ctx.chatId());
                    }
                })
                .build();
    }

    public Ability groupInfoAbility() {
        return Ability.builder()
                .name("groupinfo")
                .info("Получить подробную информацию о группе и её участниках")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        abilityBot.silent().send("Использование: /groupinfo <название_группы>", ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Optional<ChatGroup> groupOpt = groupService.getGroupByNameWithMembersAndUsers(ctx.chatId(), groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            StringBuilder response = new StringBuilder();
                            response.append("📊 *Детальная информация о группе*\n\n");
                            response.append(formatGroupInfoWithMembers(group)).append("\n\n");

                            if (group.getMembers() == null || group.getMembers().isEmpty()) {
                                response.append("👥 *Участники:* группа пуста\n");
                            } else {
                                response.append("👥 *Участники (").append(group.getMembers().size()).append("):*\n");

                                for (int i = 0; i < group.getMembers().size(); i++) {
                                    GroupMember member = group.getMembers().get(i);
                                    String userInfo = formatUserInfoForGroup(member);
                                    response.append(i + 1).append(". ").append(userInfo).append("\n");
                                }
                            }

                            abilityBot.silent().sendMd(response.toString(), ctx.chatId());
                        } else {
                            abilityBot.silent().send("❌ Группа '" + groupName + "' не найдена в этом чате", ctx.chatId());
                        }
                    } catch (Exception e) {
                        log.error("Error getting group info", e);
                        abilityBot.silent().send("❌ Ошибка при получении информации о группе: " + e.getMessage(), ctx.chatId());
                    }
                })
                .build();
    }

    public Ability addMembersAbility() {
        return Ability.builder()
                .name("addmembers")
                .info("Добавить участников в группу")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> memberOperationHandler.handleMemberOperation(ctx, true))
                .build();
    }

    public Ability removeMembersAbility() {
        return Ability.builder()
                .name("removemembers")
                .info("Удалить участников из группы")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> memberOperationHandler.handleMemberOperation(ctx, false))
                .build();
    }

    public Ability groupsSummaryAbility() {
        return Ability.builder()
                .name("groupssummary")
                .info("Краткая сводка по всем группам в чате")
                .locality(GROUP)
                .privacy(PUBLIC)
                .action(ctx -> {
                    List<ChatGroup> groups = groupService.getChatGroups(ctx.chatId());
                    if (groups.isEmpty()) {
                        abilityBot.silent().send("В этом чате еще нет групп", ctx.chatId());
                        return;
                    }

                    StringBuilder response = new StringBuilder("📊 *Сводка по группам в чате:*\n\n");

                    int totalMembers = 0;
                    for (ChatGroup group : groups) {
                        int memberCount = group.getMembers().size();
                        totalMembers += memberCount;
                        response.append("• *").append(group.getName()).append("*")
                                .append(" - ").append(memberCount).append(" участников");

                        if (group.getDescription() != null && !group.getDescription().isEmpty()) {
                            response.append(" - ").append(group.getDescription());
                        }
                        response.append("\n");
                    }

                    response.append("\n*Итого:* ").append(groups.size()).append(" групп, ")
                            .append(totalMembers).append(" участников всего");

                    abilityBot.silent().sendMd(response.toString(), ctx.chatId());
                })
                .build();
    }

    public Ability groupChatMembersAbility() {
        return Ability.builder()
                .name("groupmembers")
                .info("Получить список участников группы")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        abilityBot.silent().send("Использование: /groupmembers <название_группы>", ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Optional<ChatGroup> groupOpt = groupService.getGroupByNameWithMembersAndUsers(ctx.chatId(), groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            if (group.getMembers() == null || group.getMembers().isEmpty()) {
                                abilityBot.silent().send("👥 Группа '" + groupName + "' пуста", ctx.chatId());
                                return;
                            }

                            StringBuilder response = new StringBuilder();
                            response.append("👥 **Участники группы '").append(groupName).append("':**\n\n");

                            for (int i = 0; i < group.getMembers().size(); i++) {
                                GroupMember member = group.getMembers().get(i);
                                String userInfo = formatUserInfoForGroup(member);
                                response.append(i + 1).append(". ").append(userInfo).append("\n");
                            }

                            abilityBot.silent().send(response.toString(), ctx.chatId());
                        } else {
                            abilityBot.silent().send("❌ Группа '" + groupName + "' не найдена", ctx.chatId());
                        }
                    } catch (Exception e) {
                        log.error("Error getting group members", e);
                        abilityBot.silent().send("❌ Ошибка при получении участников группы: " + e.getMessage(), ctx.chatId());
                    }
                })
                .build();
    }

    /**
     * Форматирование информации об участнике группы для отображения
     */
    public static String formatUserInfoForGroup(GroupMember member) {
        String userName = member.getUser().getUserName();
        String firstName = member.getUser().getFirstName();
        String lastName = member.getUser().getLastName();

        StringBuilder userInfo = new StringBuilder();

        if (userName != null && !userName.isEmpty()) {
            userInfo.append("@").append(userName);
        } else {
            userInfo.append(firstName != null ? firstName : "");
            if (lastName != null && !lastName.isEmpty()) {
                if (userInfo.length() > 0) userInfo.append(" ");
                userInfo.append(lastName);
            }
        }

        userInfo.append(" (ID: ").append(member.getUser().getUserId()).append(")");

        if (Boolean.TRUE.equals(member.getUser().getIsBot())) {
            userInfo.append(" 🤖");
        }

        return userInfo.toString();
    }

    private String formatGroupInfoWithMembers(ChatGroup group) {
        int memberCount = (group.getMembers() != null) ? group.getMembers().size() : 0;

        return String.format(
                "📋 Группа: *%s*%s\n👥 Участников: %d\n🆔 ID: %d",
                Helper.escapeMarkdownV2(group.getName()),
                group.getDescription() != null ? "\n📝 Описание: " + group.getDescription() : "",
                memberCount,
                group.getId()
        );
    }
}