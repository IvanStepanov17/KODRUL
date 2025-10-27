package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.MemberManagementService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupManagementAbility implements AbilityExtension {

    private final GroupManagementService groupService;
    private final MemberManagementService memberManagementService;
    private final SendService sendService;

    public Ability createGroupAbility() {
        return Ability.builder()
                .name("creategroup")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /creategroup <название> [описание]");
                        return;
                    }

                    try {
                        String name = args[0];
                        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        String chatTitle = ctx.update().getMessage().getChat().getTitle();

                        ChatGroup group = groupService.createGroup(name, description, ctx.chatId(), chatTitle, ctx.user().getId());
                        sendService.sendMessageToThread(
                                ctx,
                                "✅ Группа создана!\n" + groupService.formatGroupInfo(group),
                                Constants.PARSE_MARKDOWN
                        );
                    } catch (Exception e) {
                        sendService.sendMessageToThread(ctx, "❌ Ошибка: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability listGroupsAbility() {
        return Ability.builder()
                .name("listgroups")
                .locality(GROUP)
                .privacy(PUBLIC)
                .action(ctx -> {
                    List<ChatGroup> groups = groupService.getChatGroups(ctx.chatId());
                    if (groups.isEmpty()) {
                        sendService.sendMessageToThread(ctx, "В этом чате еще нет групп");
                        return;
                    }

                    StringBuilder response = new StringBuilder("📋 Группы в этом чате:\n\n");
                    groups.forEach(group ->
                            response.append(groupService.formatGroupInfo(group)).append("\n\n")
                    );

                    sendService.sendMessageToThread(ctx, response.toString());
                })
                .build();
    }

    public Ability deleteGroupAbility() {
        return Ability.builder()
                .name("deletegroup")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /deletegroup <название_группы>");
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Optional<ChatGroup> groupOpt = groupService.getGroupByNameWithMembersAndUsers(ctx.chatId(), groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            // Проверяем, является ли пользователь создателем группы
                            if (!group.getCreatedBy().equals(ctx.user().getId())) {
                                // TODO Можно добавить дополнительную проверку прав администратора чата
                                sendService.sendMessageToThread(ctx, "❌ Вы можете удалять только группы, созданные вами");
                                return;
                            }

                            String groupInfo = groupService.formatGroupInfo(group);
                            groupService.deleteGroup(group.getId());

                            sendService.sendMessageToThread(ctx, "🗑️ Группа успешно удалена:\n" + groupInfo, Constants.PARSE_MARKDOWN);
                        } else {
                            sendService.sendMessageToThread(ctx, "❌ Группа '" + groupName + "' не найдена в этом чате");
                        }
                    } catch (Exception e) {
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при удалении группы: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability groupInfoAbility() {
        return Ability.builder()
                .name("groupinfo")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /groupinfo <название_группы>");
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Optional<ChatGroup> groupOpt = groupService.getGroupByNameWithMembersAndUsers(ctx.chatId(), groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            StringBuilder response = new StringBuilder();
                            response.append("📊 *Детальная информация о группе*\n\n");
                            response.append(groupService.formatGroupInfoWithMembers(group)).append("\n\n");

                            if (group.getMembers() == null || group.getMembers().isEmpty()) {
                                response.append("👥 *Участники:* группа пуста\n");
                            } else {
                                response.append("👥 *Участники (").append(group.getMembers().size()).append("):*\n");

                                for (int i = 0; i < group.getMembers().size(); i++) {
                                    GroupMember member = group.getMembers().get(i);
                                    String userInfo = groupService.formatUserInfoForGroup(member);
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

    public Ability addMembersAbility() {
        return Ability.builder()
                .name("addmembers")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> memberManagementService.handleMemberOperation(ctx, true, false))
                .build();
    }

    public Ability removeMembersAbility() {
        return Ability.builder()
                .name("removemembers")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> memberManagementService.handleMemberOperation(ctx, false, false))
                .build();
    }

    public Ability groupsSummaryAbility() {
        return Ability.builder()
                .name("groupssummary")
                .locality(GROUP)
                .privacy(PUBLIC)
                .action(ctx -> {
                    List<ChatGroup> groups = groupService.getChatGroups(ctx.chatId());
                    if (groups.isEmpty()) {
                        sendService.sendMessageToThread(ctx, "В этом чате еще нет групп");
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

                    sendService.sendMessageToThread(ctx, response.toString(), Constants.PARSE_MARKDOWN);
                })
                .build();
    }

    public Ability groupMembersAbility() {
        return Ability.builder()
                .name("groupmembers")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /groupmembers <название_группы>");
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Optional<ChatGroup> groupOpt = groupService.getGroupByNameWithMembersAndUsers(ctx.chatId(), groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup group = groupOpt.get();

                            if (group.getMembers() == null || group.getMembers().isEmpty()) {
                                sendService.sendMessageToThread(ctx, "👥 Группа '" + groupName + "' пуста");
                                return;
                            }

                            StringBuilder response = new StringBuilder();
                            response.append("👥 *Участники группы '").append(groupName).append("':*\n\n");

                            for (int i = 0; i < group.getMembers().size(); i++) {
                                GroupMember member = group.getMembers().get(i);
                                String userInfo = groupService.formatUserInfoForGroup(member);
                                response.append(i + 1).append(". ").append(userInfo).append("\n");
                            }
                            sendService.sendMessageToThread(ctx, response.toString());
                        } else {
                            sendService.sendMessageToThread(ctx, "❌ Группа '" + groupName + "' не найдена");
                        }
                    } catch (Exception e) {
                        log.error("Error getting group members", e);
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при получении участников группы: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability tagUserAbility() {
        return Ability.builder()
                .name("tag")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length != 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /tag <название группы>");
                        return;
                    }

                    try {
                        Long chatId = ctx.chatId();
                        Integer messageThreadId = ctx.update().getMessage().getMessageThreadId();
                        String groupName = args[0];

                        log.info("Tag command executed in chat: {}, thread: {} for group: {}",
                                chatId, messageThreadId, groupName);

                        Optional<ChatGroup> groupOpt = groupService.getGroupByNameWithMembersAndUsers(chatId, groupName);

                        if (groupOpt.isPresent()) {
                            ChatGroup chatGroup = groupOpt.get();
                            String userNames = groupService.getTagUsersMessage(chatGroup);

                            if (userNames != null && !userNames.trim().isEmpty()) {
                                sendService.sendMessageToThread(ctx, userNames);
                            } else {
                                sendService.sendMessageToThread(ctx, "❌ Группа пуста");
                            }
                        } else {
                            sendService.sendMessageToThread(ctx,
                                    "❌ Группа '" + groupName + "' не найдена в этом чате");
                        }

                    } catch (Exception e) {
                        log.error("Error in tag command for chat {}: {}", ctx.chatId(), e.getMessage(), e);
                        sendService.sendMessageToThread(ctx, "❌ Ошибка: " + e.getMessage());
                    }
                })
                .build();
    }
}