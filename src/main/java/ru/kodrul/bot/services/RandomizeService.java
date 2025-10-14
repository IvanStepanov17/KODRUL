package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.springframework.transaction.annotation.Transactional;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.utils.EscapeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomizeService {

    private final GroupManagementService groupManagementService;

    @Transactional(readOnly = true)
    public void replayRandomize(MessageContext context, SilentSender sender) {
        String[] args = context.arguments();

        if (args.length == 0) {
            sender.send("Для выбора по одному участнику по каждому направлению укажите по каким направлениям "
                    + "требуется выбор. Пример: /randomize qa front back ann", context.chatId());
            return;
        }

        StringBuilder builder = new StringBuilder().append("🎲 Результаты рандомизации:\n\n");

        boolean hasValidArguments = false;

        for (String arg : args) {
            try {
                Optional<ChatGroup> groupOpt = groupManagementService.getGroupByNameWithMembersAndUsers(context.chatId(), arg);
                if (groupOpt.isPresent()) {
                    ChatGroup group = groupOpt.get();
                    List<GroupMember> members = group.getMembers();

                    if (members.isEmpty()) {
                        builder.append(String.format("❌ Группа '%s' пуста\n", arg));
                        continue;
                    }

                    // Перемешиваем участников
                    List<GroupMember> shuffledMembers = new ArrayList<>(members);
                    Collections.shuffle(shuffledMembers);

                    // Берем первого участника из перемешанного списка
                    GroupMember selectedMember = shuffledMembers.get(0);
                    String userName = formatUserName(selectedMember);

                    builder.append(String.format("🏷️ *%s*: %s\n", EscapeHelper.escapeMarkdownV2(group.getName()), EscapeHelper.escapeMarkdownV2(userName)));
                    hasValidArguments = true;

                } else {
                    builder.append(String.format("❌ Группа '%s' не найдена\n", arg));
                }
            } catch (Exception e) {
                log.error("Error processing argument '{}'", arg, e);
                builder.append(String.format("❌ Ошибка при обработке '%s'\n", arg));
            }
        }

        if (!hasValidArguments) {
            sender.send("❌ Укажите верные названия групп для рандомизации. Используйте /listgroups чтобы увидеть доступные группы.", context.chatId());
        } else {
            sender.sendMd(builder.toString(), context.chatId());
        }
    }

    /**
     * Форматирование имени пользователя для отображения
     */
    private String formatUserName(GroupMember member) {
        String userName = member.getUser().getUserName();
        String firstName = member.getUser().getFirstName();
        String lastName = member.getUser().getLastName();

        if (userName != null && !userName.isEmpty()) {
            return "@" + userName;
        } else {
            return String.format("%s %s",
                    firstName != null ? firstName : "",
                    lastName != null ? lastName : "").trim();
        }
    }

    /**
     * Дополнительный метод для рандомизации нескольких участников из группы
     */
    @Transactional(readOnly = true)
    public void randomizeMultipleFromGroup(MessageContext context, SilentSender sender, String groupName, int count) {
        try {
            Optional<ChatGroup> groupOpt = groupManagementService.getGroupByNameWithMembersAndUsers(context.chatId(), groupName);
            if (groupOpt.isEmpty()) {
                sender.send("❌ Группа '" + groupName + "' не найдена", context.chatId());
                return;
            }

            ChatGroup group = groupOpt.get();
            List<GroupMember> members = group.getMembers();

            if (members.isEmpty()) {
                sender.send("❌ Группа '" + groupName + "' пуста", context.chatId());
                return;
            }

            if (count > members.size()) {
                sender.send(String.format("❌ В группе только %d участников, нельзя выбрать %d",
                        members.size(), count), context.chatId());
                return;
            }

            // Перемешиваем и выбираем нужное количество
            List<GroupMember> shuffledMembers = new ArrayList<>(members);
            Collections.shuffle(shuffledMembers);

            StringBuilder result = new StringBuilder();
            result.append(String.format("🎲 Случайные %d участников из группы '%s':\n\n", count, groupName));

            for (int i = 0; i < count; i++) {
                GroupMember member = shuffledMembers.get(i);
                String userName = formatUserName(member);
                result.append(String.format("%d. %s\n", i + 1, userName));
            }

            sender.sendMd(result.toString(), context.chatId());

        } catch (Exception e) {
            log.error("Error in randomizeMultipleFromGroup", e);
            sender.send("❌ Ошибка при выполнении рандомизации", context.chatId());
        }
    }

    /**
     * Метод для распределения участников группы по командам
     */
    @Transactional(readOnly = true)
    public void distributeGroupToTeams(MessageContext context, SilentSender sender, String groupName, int teamCount) {
        try {
            Optional<ChatGroup> groupOpt = groupManagementService.getGroupByNameWithMembersAndUsers(context.chatId(), groupName);
            if (groupOpt.isEmpty()) {
                sender.send("❌ Группа '" + groupName + "' не найдена", context.chatId());
                return;
            }

            ChatGroup group = groupOpt.get();
            List<GroupMember> members = group.getMembers();

            if (members.isEmpty()) {
                sender.send("❌ Группа '" + groupName + "' пуста", context.chatId());
                return;
            }

            if (teamCount < 2 || teamCount > members.size()) {
                sender.send("❌ Количество команд должно быть от 2 до " + members.size(), context.chatId());
                return;
            }

            // Перемешиваем участников
            List<GroupMember> shuffledMembers = new ArrayList<>(members);
            Collections.shuffle(shuffledMembers);

            // Создаем команды
            List<List<GroupMember>> teams = new ArrayList<>();
            for (int i = 0; i < teamCount; i++) {
                teams.add(new ArrayList<>());
            }

            // Распределяем участников по командам
            for (int i = 0; i < shuffledMembers.size(); i++) {
                int teamIndex = i % teamCount;
                teams.get(teamIndex).add(shuffledMembers.get(i));
            }

            // Формируем результат
            StringBuilder result = new StringBuilder();
            result.append(String.format("🏆 Распределение группы '%s' на %d команд:\n\n", groupName, teamCount));

            for (int i = 0; i < teams.size(); i++) {
                result.append(String.format("*Команда %d* (%d участников):\n", i + 1, teams.get(i).size()));

                for (GroupMember member : teams.get(i)) {
                    String userName = EscapeHelper.escapeMarkdownV2(formatUserName(member));
                    result.append("• ").append(userName).append("\n");
                }
                result.append("\n");
            }

            sender.sendMd(result.toString(), context.chatId());

        } catch (Exception e) {
            log.error("Error in distributeGroupToTeams", e);
            sender.send("❌ Ошибка при распределении по командам", context.chatId());
        }
    }
}