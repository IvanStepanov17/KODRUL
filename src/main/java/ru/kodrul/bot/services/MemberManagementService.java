package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.entity.TelegramUser;
import ru.kodrul.bot.parser.MentionParser;
import ru.kodrul.bot.pojo.OperationResult;
import ru.kodrul.bot.pojo.ParsedMention;
import ru.kodrul.bot.utils.EscapeHelper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberManagementService {

    @Lazy
    private final AbilityBot bot;
    private final GroupManagementService groupService;
    private final UserSyncService userSyncService;
    private final MentionParser mentionParser;

    @Transactional
    public void handleMemberOperation(MessageContext ctx, boolean isAdd, boolean isTrustedCommand) {
        String[] args = ctx.arguments();

        // TODO изменить использование при вызове от доверенного пользователя с передачей идентификатора чата
        if (args.length < 1) {
            String usage = isAdd ?
                    "Использование: /addmembers <имя_группы> @user1 @user2 ..." :
                    "Использование: /removemembers <имя_группы> @user1 @user2 ...";
            bot.silent().send(usage, ctx.chatId());
            return;
        }

        String groupName = args[0];
        Long chatId;
        if (isTrustedCommand) {
            chatId = Long.valueOf(args[1]);
        } else {
            chatId = ctx.chatId();
        }

        try {
            Optional<ChatGroup> groupOpt = groupService.getGroupByName(chatId, groupName);
            if (groupOpt.isEmpty()) {
                bot.silent().send("❌ Группа '" + groupName + "' не найдена в этом чате", ctx.chatId());
                return;
            }

            ChatGroup group = groupOpt.get();
            MessageEntity[] entities = ctx.update().getMessage().getEntities() != null ?
                    ctx.update().getMessage().getEntities().toArray(new MessageEntity[0]) : new MessageEntity[0];

            List<ParsedMention> mentions = mentionParser.parseMentions(
                    ctx.update().getMessage().getText(),
                    List.of(entities)
            );

            if (mentions.isEmpty()) {
                bot.silent().send("❌ Не найдено упоминаний пользователей. Упомяните пользователей через @username", ctx.chatId());
                return;
            }

            OperationResult result = isAdd ?
                    addMembersToGroup(group, mentions, chatId) :
                    removeMembersFromGroup(group, mentions);

            sendOperationResult(ctx, result, groupName, isAdd);

        } catch (Exception e) {
            log.error("Error handling member operation", e);
            bot.silent().send("❌ Ошибка при обработке операции: " + e.getMessage(), ctx.chatId());
        }
    }

    private OperationResult addMembersToGroup(ChatGroup group, List<ParsedMention> mentions, Long chatId) {
        OperationResult result = new OperationResult();

        for (ParsedMention mention : mentions) {
            try {
                if (mention.getUserId() != null) {
                    addUserToGroup(group, mention.getUserId(), result);
                } else {
                    addUserByUsername(group, mention.getUsername(), chatId, result);
                }
            } catch (Exception e) {
                result.addFailed(mention.getText(), e.getMessage());
                log.warn("Failed to add user {}: {}", mention.getText(), e.getMessage());
            }
        }

        return result;
    }

    private void addUserToGroup(ChatGroup group, Long userId, OperationResult result) {
        try {
            TelegramUser user = userSyncService.syncUserWithChat(userId, group.getChatId());
            groupService.addUserToGroup(group.getId(), userId);

            String userName = user.getUserName() != null ?
                    "@" + user.getUserName() :
                    user.getFirstName() + " " + user.getLastName();
            result.addSuccess(userName);

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("уже в группе")) {
                result.addSkipped("Пользователь уже в группе");
            } else {
                throw e;
            }
        }
    }

    private void addUserByUsername(ChatGroup group, String username, Long chatId, OperationResult result) {
        Optional<TelegramUser> userOpt = userSyncService.findUserByUsername(username);

        if (userOpt.isPresent()) {
            addUserToGroup(group, userOpt.get().getUserId(), result);
        } else {
            result.addFailed("@" + username, "Пользователь не найден в базе");
        }
    }

    private OperationResult removeMembersFromGroup(ChatGroup group, List<ParsedMention> mentions) {
        OperationResult result = new OperationResult();

        List<GroupMember> currentMembers = groupService.getGroupMembersWithUsers(group.getId());
        var currentUserIds = currentMembers.stream()
                .map(member -> member.getUser().getUserId())
                .collect(Collectors.toSet());

        for (ParsedMention mention : mentions) {
            try {
                Long userIdToRemove = findUserIdToRemove(mention, currentUserIds);

                if (userIdToRemove != null) {
                    groupService.removeMemberFromGroupWithUser(group.getId(), userIdToRemove);
                    result.addSuccess(mention.getText());
                } else {
                    result.addSkipped("Пользователь не найден в группе");
                }
            } catch (Exception e) {
                result.addFailed(mention.getText(), e.getMessage());
            }
        }

        return result;
    }

    private Long findUserIdToRemove(ParsedMention mention, Set<Long> currentUserIds) {
        if (mention.getUserId() != null) {
            return currentUserIds.contains(mention.getUserId()) ? mention.getUserId() : null;
        } else {
            var userOpt = userSyncService.findUserByUsername(mention.getUsername());
            return userOpt.filter(user -> currentUserIds.contains(user.getUserId()))
                    .map(TelegramUser::getUserId)
                    .orElse(null);
        }
    }

    private void sendOperationResult(MessageContext ctx, OperationResult result, String groupName, boolean isAdd) {
        String action = isAdd ? "добавления в группу" : "удаления из группы";
        StringBuilder response = new StringBuilder();

        response.append(String.format("📋 Результат %s '%s':\n\n", action, groupName));

        if (!result.getSuccess().isEmpty()) {
            response.append("✅ *Успешно:*\n");
            result.getSuccess().forEach(user -> response.append("• ").append(EscapeHelper.escapeMarkdownV2(user)).append("\n"));
            response.append("\n");
        }

        if (!result.getSkipped().isEmpty()) {
            response.append("⚠️ *Пропущено:*\n");
            result.getSkipped().forEach(reason -> response.append("• ").append(reason).append("\n"));
            response.append("\n");
        }

        if (!result.getFailed().isEmpty()) {
            response.append("❌ *Ошибки:*\n");
            result.getFailed().forEach((user, error) ->
                    response.append("• ").append(EscapeHelper.escapeMarkdownV2(user)).append(": ").append(error).append("\n"));
        }

        if (result.getSuccess().isEmpty() && result.getSkipped().isEmpty() && result.getFailed().isEmpty()) {
            response.append("ℹ️ Не было обработано ни одного пользователя");
        }

        bot.silent().sendMd(response.toString(), ctx.chatId());
    }
}