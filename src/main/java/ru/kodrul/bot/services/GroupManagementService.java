package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.GroupMember;
import ru.kodrul.bot.entity.TelegramUser;
import ru.kodrul.bot.repository.ChatGroupRepository;
import ru.kodrul.bot.repository.GroupMemberRepository;
import ru.kodrul.bot.repository.TelegramUserRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupManagementService {

    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final TelegramUserRepository userRepository;

    @Transactional
    public ChatGroup createGroup(String name, String description, Long chatId, String chatTitle, Long createdBy) {
        if (groupRepository.existsByChatIdAndName(chatId, name)) {
            throw new IllegalArgumentException("Группа с именем '" + name + "' уже существует в этом чате");
        }

        ChatGroup group = new ChatGroup();
        group.setName(name);
        group.setDescription(description);
        group.setChatId(chatId);
        group.setChatTitle(chatTitle);
        group.setCreatedBy(createdBy);

        return groupRepository.save(group);
    }

    @Transactional
    public void addUserToGroup(Long groupId, Long userId) {
        if (memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("Пользователь уже в группе");
        }

        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        TelegramUser user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);

        memberRepository.save(member);
        log.info("User {} added to group {}", userId, groupId);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        groupRepository.deleteById(groupId);
    }

    @Transactional(readOnly = true)
    public Optional<ChatGroup> getGroupByNameWithMembersAndUsers(Long chatId, String name) {
        return groupRepository.findByChatIdAndNameWithMembersAndUsers(chatId, name);
    }

    @Transactional(readOnly = true)
    public List<GroupMember> getGroupMembersWithUsers(Long groupId) {
        Optional<ChatGroup> groupOpt = groupRepository.findByIdWithMembersAndUsers(groupId);
        return groupOpt.map(ChatGroup::getMembers).orElse(Collections.emptyList());
    }

    @Transactional
    public void removeMemberFromGroupWithUser(Long groupId, Long userId) {
        if (!isUserInGroupWithUser(groupId, userId)) {
            throw new IllegalArgumentException("Пользователь не найден в группе");
        }

        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("User {} removed from group {}", userId, groupId);
    }

    @Transactional(readOnly = true)
    public boolean isUserInGroupWithUser(Long groupId, Long userId) {
        return memberRepository.findByGroupIdAndUserIdWithUser(groupId, userId).isPresent();
    }

    public List<ChatGroup> getChatGroups(Long chatId) {
        return groupRepository.findByChatIdWithMembers(chatId);
    }

    public Optional<ChatGroup> getGroupByName(Long chatId, String name) {
        return groupRepository.findByChatIdAndName(chatId, name);
    }

    public String formatGroupInfo(ChatGroup group) {
        return String.format(
                "📋 Группа: *%s*%s\n👥 Участников: %d\n🆔 ID: %d",
                group.getName(),
                group.getDescription() != null ? "\n📝 Описание: " + group.getDescription() : "",
                group.getMembers().size(),
                group.getId()
        );
    }

    /**
     * Получение статистики по группам в чате
     */
    public Map<String, Object> getGroupsStatistics(Long chatId) {
        List<ChatGroup> groups = groupRepository.findByChatIdWithMembers(chatId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGroups", groups.size());

        int totalMembers = groups.stream()
                .mapToInt(group -> group.getMembers().size())
                .sum();
        stats.put("totalMembers", totalMembers);

        // Статистика по ботам
        long totalBots = groups.stream()
                .flatMap(group -> group.getMembers().stream())
                .filter(member -> Boolean.TRUE.equals(member.getUser().getIsBot()))
                .count();
        stats.put("totalBots", totalBots);

        // Самая большая и самая маленькая группа
        if (!groups.isEmpty()) {
            ChatGroup largestGroup = groups.stream()
                    .max(Comparator.comparingInt(group -> group.getMembers().size()))
                    .orElse(groups.get(0));
            ChatGroup smallestGroup = groups.stream()
                    .min(Comparator.comparingInt(group -> group.getMembers().size()))
                    .orElse(groups.get(0));

            stats.put("largestGroup", largestGroup.getName());
            stats.put("largestGroupSize", largestGroup.getMembers().size());
            stats.put("smallestGroup", smallestGroup.getName());
            stats.put("smallestGroupSize", smallestGroup.getMembers().size());
        }

        return stats;
    }

    /**
     * Поиск групп по участнику
     */
    public List<ChatGroup> findGroupsByMember(Long chatId, Long userId) {
        return groupRepository.findByChatIdWithMembers(chatId).stream()
                .filter(group -> group.getMembers().stream()
                        .anyMatch(member -> member.getUser().getUserId().equals(userId)))
                .collect(Collectors.toList());
    }

    /**
     * Получение группы по ID с проверкой прав доступа
     */
    public Optional<ChatGroup> getGroupWithAccessCheck(Long groupId, Long userId, Long chatId) {
        Optional<ChatGroup> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            ChatGroup group = groupOpt.get();
            // Проверяем, что группа принадлежит этому чату
            if (group.getChatId().equals(chatId)) {
                // TODO Нужна ли дополнительная проверка прав доступа?
                return groupOpt;
            }
        }
        return Optional.empty();
    }

    public boolean isUserInGroup(Long groupId, Long userId) {
        return memberRepository.existsByGroupIdAndUserId(groupId, userId);
    }
}