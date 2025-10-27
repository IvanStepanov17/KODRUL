package ru.kodrul.bot.abilities.hidden;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.common.CommonAbilityHelper;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.parser.CommandParser;
import ru.kodrul.bot.pojo.CommandArguments;
import ru.kodrul.bot.services.AuthorizationService;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.ScheduledPostService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAbilityHidden implements AbilityExtension {

    private final ScheduledPostService scheduledPostService;
    private final GroupManagementService groupManagementService;
    private final AuthorizationService authorizationService;
    private final SendService sendService;
    private final CommonAbilityHelper commonAbilityHelper;
    private final CommandParser commandParser;

    public Ability createScheduleHiddenAbility() {
        return Ability.builder()
                .name("createschedulehidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx -> {
                    String fullText = ctx.update().getMessage().getText();
                    Long userId = ctx.user().getId();

                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        log.warn("Unauthorized schedule access attempt by user: {}", userId);
                        return;
                    }

                    try {
                        // Парсим аргументы с поддержкой кавычек
                        CommandArguments args = commandParser.parseCommandWithQuotes(fullText);

                        if (args.getChatId() == null || args.getGroupName() == null ||
                                args.getSchedule() == null || args.getMessage() == null) {
                            sendScheduleHiddenHelp(userId);
                            return;
                        }

                        Long targetChatId = args.getChatId();
                        String groupName = args.getGroupName();
                        String scheduleInput = args.getSchedule();
                        String messageText = args.getMessage();
                        String imageUrl = args.getImageUrl();

                        if (!commonAbilityHelper.isBotMemberOfChat(targetChatId)) {
                            sendService.sendToUser(userId,
                                    "❌ Бот не является участником указанного чата или чат не существует\n" +
                                            "Chat ID: " + targetChatId);
                            return;
                        }

                        Optional<ChatGroup> groupOpt = groupManagementService.getGroupByName(targetChatId, groupName);
                        if (groupOpt.isEmpty()) {
                            sendService.sendToUser(userId,
                                    "❌ Группа '" + groupName + "' не найдена в указанном чате\n\n" +
                                            "Сначала создайте группу командой:\n" +
                                            "/creategrouphidden " + targetChatId + " " + groupName + " \"Описание группы\"");
                            return;
                        }

                        String chatTitle = commonAbilityHelper.getChatTitle(targetChatId);

                        ScheduledPost schedule = scheduledPostService.createSchedule(
                                targetChatId, groupName, scheduleInput, messageText, imageUrl, userId
                        );

                        String successMessage = String.format(
                                """
                                        ✅ Расписание создано!

                                        📋 Группа: %s
                                        💬 Чат: %s (ID: %d)
                                        ⏰ Расписание: %s
                                        ✉️ Сообщение: %s
                                        %s
                                        👤 Создано: %s (ID: %d)
                                        🆔 ID расписания: %d""",
                                groupName,
                                chatTitle,
                                targetChatId,
                                schedule.getDescription(),
                                messageText,
                                imageUrl != null ? "🖼️ Изображение: " + imageUrl + "\n" : "",
                                ctx.user().getFirstName(),
                                userId,
                                schedule.getId()
                        );

                        sendService.sendToUser(userId, successMessage);
                        log.info("Schedule created by user {} for chat {}: {}",
                                userId, targetChatId, schedule.getDescription());

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ Chat ID должен быть числом");
                    } catch (Exception e) {
                        log.error("Error creating schedule for user {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка при создании расписания: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability listSchedulesHiddenAbility() {
        return Ability.builder()
                .name("listscheduleshidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        return;
                    }

                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendToUser(userId,
                                """
                                        Использование: /listscheduleshidden <chat_id>

                                        Пример: /listscheduleshidden -100123456789""");
                        return;
                    }

                    try {
                        Long targetChatId = Long.parseLong(args[0]);
                        String chatTitle = commonAbilityHelper.getChatTitle(targetChatId);

                        var schedules = scheduledPostService.getActiveSchedulesForChat(targetChatId);

                        if (schedules.isEmpty()) {
                            sendService.sendToUser(userId,
                                    "📭 В чате '" + chatTitle + "' нет активных расписаний");
                            return;
                        }

                        StringBuilder response = new StringBuilder();
                        response.append("📅 *Расписания для чата:*\n")
                                .append("💬 *Чат:* ").append(chatTitle).append(" (ID: ").append(targetChatId).append(")\n\n");

                        for (ScheduledPost schedule : schedules) {
                            response.append("🆔 *ID:* ").append(schedule.getId()).append("\n")
                                    .append("📋 *Группа:* ").append(schedule.getGroupName()).append("\n")
                                    .append("⏰ *Когда:* ").append(schedule.getDescription()).append("\n")
                                    .append("💬 *Сообщение:* ").append(
                                            schedule.getMessageText().length() > 50 ?
                                                    schedule.getMessageText().substring(0, 50) + "..." :
                                                    schedule.getMessageText()
                                    ).append("\n");

                            if (schedule.getImageUrl() != null) {
                                response.append("🖼️ *Изображение:* есть\n");
                            }

                            if (schedule.getLastSent() != null) {
                                response.append("✅ *Последняя отправка:* ").append(schedule.getLastSent().toLocalDate()).append("\n");
                            }

                            response.append("---\n");
                        }

                        response.append("\n💡 *Управление расписаниями:*\n")
                                .append("/toggleschedulehidden <ID> on/off - вкл/выкл\n")
                                .append("/deleteschedulehidden <ID> - удалить\n")
                                .append("/scheduleinfohidden <ID> - подробности");

                        sendService.sendToUser(userId, response.toString(), Constants.PARSE_MARKDOWN);

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ Chat ID должен быть числом");
                    } catch (Exception e) {
                        log.error("Error listing schedules for user {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка при получении расписаний: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability toggleScheduleHiddenAbility() {
        return Ability.builder()
                .name("toggleschedulehidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(2)
                .action(ctx -> {
                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        return;
                    }

                    String[] args = ctx.arguments();
                    if (args.length < 2) {
                        sendService.sendToUser(userId, "Использование: /toggleschedulehidden <ID_расписания> <on|off>");
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);
                        boolean isActive = "on".equalsIgnoreCase(args[1]);

                        var scheduleOpt = scheduledPostService.findById(scheduleId);
                        if (scheduleOpt.isEmpty()) {
                            sendService.sendToUser(userId, "❌ Расписание с ID " + scheduleId + " не найдено");
                            return;
                        }

                        scheduledPostService.toggleSchedule(scheduleId, isActive);

                        ScheduledPost schedule = scheduleOpt.get();
                        String chatTitle = commonAbilityHelper.getChatTitle(schedule.getChatId());

                        String message = String.format(
                                """
                                        %s *Расписание %s!*

                                        💬 *Чат:* %s
                                        📋 *Группа:* %s
                                        ⏰ *Расписание:* %s
                                        🆔 *ID:* %d""",
                                isActive ? "✅" : "⏸️",
                                isActive ? "включено" : "выключено",
                                chatTitle,
                                schedule.getGroupName(),
                                schedule.getDescription(),
                                scheduleId
                        );

                        sendService.sendToUser(userId, message, Constants.PARSE_MARKDOWN);
                        log.info("User {} toggled schedule {} to {}", userId, scheduleId, isActive);

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ ID расписания должен быть числом");
                    } catch (Exception e) {
                        log.error("Error toggling schedule for user {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка при изменении расписания: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability deleteScheduleHiddenAbility() {
        return Ability.builder()
                .name("deleteschedulehidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        return;
                    }

                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendToUser(userId, "Использование: /deleteschedulehidden <ID_расписания>");
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);

                        var scheduleOpt = scheduledPostService.findById(scheduleId);
                        if (scheduleOpt.isEmpty()) {
                            sendService.sendToUser(userId, "❌ Расписание с ID " + scheduleId + " не найдено");
                            return;
                        }

                        ScheduledPost schedule = scheduleOpt.get();
                        String chatTitle = commonAbilityHelper.getChatTitle(schedule.getChatId());

                        scheduledPostService.deleteSchedule(scheduleId);

                        String message = String.format(
                                """
                                        🗑️ *Расписание удалено!*

                                        💬 *Чат:* %s
                                        📋 *Группа:* %s
                                        ⏰ *Расписание:* %s
                                        🆔 *ID:* %d""",
                                chatTitle,
                                schedule.getGroupName(),
                                schedule.getDescription(),
                                scheduleId
                        );

                        sendService.sendToUser(userId, message, Constants.PARSE_MARKDOWN);
                        log.info("User {} deleted hidden schedule {}", userId, scheduleId);

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ ID расписания должен быть числом");
                    } catch (Exception e) {
                        log.error("Error deleting hidden schedule for user {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка при удалении расписания: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability scheduleInfoHiddenAbility() {
        return Ability.builder()
                .name("scheduleinfohidden")
                .locality(USER)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        return;
                    }

                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendToUser(userId, "Использование: /scheduleinfohidden <ID_расписания>");
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);
                        var scheduleOpt = scheduledPostService.findById(scheduleId);

                        if (scheduleOpt.isPresent()) {
                            ScheduledPost schedule = scheduleOpt.get();
                            String chatTitle = commonAbilityHelper.getChatTitle(schedule.getChatId());

                            StringBuilder response = new StringBuilder();
                            response.append("📊 *Информация о скрытом расписании:*\n\n")
                                    .append("🆔 *ID:* ").append(schedule.getId()).append("\n")
                                    .append("💬 *Чат:* ").append(chatTitle).append(" (ID: ").append(schedule.getChatId()).append(")\n")
                                    .append("📋 *Группа:* ").append(schedule.getGroupName()).append("\n")
                                    .append("⏰ *Описание:* ").append(schedule.getDescription()).append("\n")
                                    .append("⚙️ *Cron выражение:* `").append(schedule.getCronExpression()).append("`\n")
                                    .append("💬 *Сообщение:* ").append(schedule.getMessageText()).append("\n")
                                    .append("🖼️ *Изображение:* ").append(schedule.getImageUrl() != null ? schedule.getImageUrl() : "нет").append("\n")
                                    .append("📊 *Статус:* ").append(schedule.getIsActive() ? "активно ✅" : "неактивно ⏸️").append("\n")
                                    .append("👤 *Создано:* ").append(schedule.getCreatedBy()).append("\n")
                                    .append("📅 *Создано:* ").append(schedule.getCreatedAt().toLocalDate()).append("\n");

                            if (schedule.getLastSent() != null) {
                                response.append("✅ *Последняя отправка:* ").append(schedule.getLastSent()).append("\n");
                            }

                            sendService.sendToUser(userId, response.toString(), Constants.PARSE_MARKDOWN);
                        } else {
                            sendService.sendToUser(userId, "❌ Расписание с ID " + scheduleId + " не найдено");
                        }

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ ID расписания должен быть числом");
                    } catch (Exception e) {
                        log.error("Error getting schedule info for user {}: {}", userId, e.getMessage(), e);
                        sendService.sendToUser(userId, "❌ Ошибка при получении информации: " + e.getMessage());
                    }
                })
                .build();
    }

    private void sendScheduleHiddenHelp(Long userId) {
        String helpText = """
            📅 *Использование:* /createschedulehidden <chat_id> <группа> "<расписание>" <сообщение>
            
            *ОБРАТИТЕ ВНИМАНИЕ:* Расписание и текст сообщения должны быть в кавычках!
            
            *Простые форматы:*
            • `"09:00"` - ежедневно в 9:00
            • `"еженедельно пн,ср,пт 09:00"` - по понедельникам, средам и пятницам в 9:00
            • `"ежемесячно 1,15 09:00"` - 1 и 15 числа каждого месяца в 9:00
            
            *Cron-выражения (для продвинутых):*
            • `"0 0 9 * * ?"` - ежедневно в 9:00
            • `"0 0 9 ? * MON,WED,FRI"` - по пн, ср, пт в 9:00
            • `"0 0 9 1 * ?"` - первое число месяца в 9:00
            • `"0 0 12 ? * SUN"` - каждое воскресенье в 12:00
            
            *Дни недели:* пн, вт, ср, чт, пт, сб, вс
            
            *Примеры:*
            /createschedulehidden -100123456789 Тест "09:00" "Доброе утро!"
            /createschedulehidden -100123456789 Созвон "еженедельно пн,ср,пт 10:30" "Время созвона!"
            /createschedulehidden -100123456789 Отчет "ежемесячно 1 09:00" "Ежемесячный отчет"
            /createschedulehidden -100123456789 Обед "0 0 12 * * ?" "Время обеда!"
            
            *Для URL изображения добавьте его в конце:*
            /createschedulehidden -100123456789 Тест "09:00" "Доброе утро!" https://example.com/image.jpg
            """;

        sendService.sendToUser(userId, helpText, Constants.PARSE_MARKDOWN);
    }
}