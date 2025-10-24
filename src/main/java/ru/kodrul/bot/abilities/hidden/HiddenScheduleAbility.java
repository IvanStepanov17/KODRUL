package ru.kodrul.bot.abilities.hidden;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.common.CommonAbilityHelper;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.ScheduledPost;
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
public class HiddenScheduleAbility implements AbilityExtension {

    private final ScheduledPostService scheduledPostService;
    private final GroupManagementService groupManagementService;
    private final AuthorizationService authorizationService;
    private final SendService sendService;
    private final CommonAbilityHelper commonAbilityHelper;

    public Ability createScheduleHiddenAbility() {
        return Ability.builder()
                .name("createschedulehidden")
                .info("Создать расписание для указанного чата (только для доверенных пользователей)")
                .locality(USER)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx -> {
                    String fullText = ctx.update().getMessage().getText();
                    String[] parts = fullText.split("\\s+", 5);

                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        log.warn("Unauthorized schedule access attempt by user: {}", userId);
                        return;
                    }

                    if (parts.length < 5) {
                        sendService.sendToUser(userId,
                                """
                                        📅 *Использование:* /createschedulehidden <chat_id> <группа> <расписание> <сообщение>

                                        *Простые форматы:*
                                        • `09:00` - ежедневно в 9:00
                                        • `пн,ср,пт 09:00` - по понедельникам, средам и пятницам в 9:00
                                        • `1,15 09:00` - 1 и 15 числа каждого месяца в 9:00
                                        • `еженедельно вc 12:00` - каждое воскресенье в 12:00
                                        
                                        *Cron-выражения (для продвинутых):*
                                        • `0 0 9 * * ?` - ежедневно в 9:00
                                        • `0 0 9 ? * MON,WED,FRI` - по понедельникам, средам, пятницам в 9:00
                                        • `0 0 9 1 * ?` - первое число месяца в 9:00
                                        • `0 0 12 ? * SUN` - каждое воскресенье в 12:00
                                        
                                        *Дни недели:* пн, вт, ср, чт, пт, сб, вс

                                        *Примеры:*
                                        /createschedulehidden -100123456789 Тест 09:00 "Доброе утро!"
                                        /createschedulehidden -100123456789 Созвон "пн,ср,пт 10:30" "Время созвона!"

                                        *Для URL изображения добавьте его в конце через пробел*""",
                                Constants.PARSE_MARKDOWN);
                        return;
                    }

                    try {
                        Long targetChatId = Long.parseLong(parts[1]);
                        String groupName = parts[2];
                        String scheduleInput = parts[3];
                        String restOfText = parts[4];

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

                        String messageText;
                        String imageUrl = null;

                        String[] textParts = restOfText.split("\\s+");
                        if (textParts.length > 1 &&
                                (textParts[textParts.length - 1].startsWith("http://") ||
                                        textParts[textParts.length - 1].startsWith("https://"))) {
                            imageUrl = textParts[textParts.length - 1];
                            messageText = restOfText.substring(0, restOfText.lastIndexOf(imageUrl)).trim();
                        } else {
                            messageText = restOfText;
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
                                imageUrl != null ? "🖼️ Изображение: + imageUrl + \n" : "",
                                ctx.user().getFirstName(),
                                userId,
                                schedule.getId()
                        );

                        sendService.sendToUser(userId, successMessage);
                        log.info("Schedule created by user {} for chat {}: {}",
                                userId, targetChatId, schedule.getDescription());

                    } catch (NumberFormatException e) {
                        sendService.sendToUser(userId, "❌ Chat ID должен быть числом");
                    } catch (IllegalArgumentException e) {
                        sendService.sendToUser(userId, "❌ " + e.getMessage() + "\n\nИспользуйте /schedulehelp для справки по форматам расписания");
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
                .info("Показать расписания для указанного чата")
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
                .info("Включить/выключить указанное расписание")
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
                .info("Удалить указанное расписание")
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
                .info("Получить подробную информацию о расписании")
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
}