package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.ScheduledPostService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;
import ru.kodrul.bot.utils.EscapeHelper;

import java.util.List;
import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAbility implements AbilityExtension {

    private final ScheduledPostService scheduledPostService;
    private final GroupManagementService groupManagementService;
    private final SendService sendService;

    public Ability createScheduleAbility() {
        return Ability.builder()
                .name("createschedule")
                .info("Создать расписание - укажите группу, расписание, а затем сообщение")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> {
                    String fullText = ctx.update().getMessage().getText();
                    String[] parts = fullText.split("\\s+", 4);

                    if (parts.length < 4) {
                        sendScheduleHelp(ctx);
                        return;
                    }

                    try {
                        String groupName = parts[1];
                        String scheduleInput = parts[2];
                        String restOfText = EscapeHelper.escapeMarkdownV2(parts[3]);

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

                        ScheduledPost schedule = scheduledPostService.createSchedule(
                                ctx.chatId(), groupName, scheduleInput, messageText, imageUrl, ctx.user().getId()
                        );

                        String response = String.format(
                                """
                                        ✅ *Расписание создано!*

                                        📋 *Группа:* %s
                                        📅 *Расписание:* %s
                                        💬 *Сообщение:* %s
                                        %s\
                                        🆔 *ID:* %d""",
                                groupName,
                                schedule.getDescription(),
                                messageText,
                                imageUrl != null ? "🖼️ *Изображение:* есть\n" : "",
                                schedule.getId()
                        );

                        sendService.sendMessageToThread(ctx, response, Constants.PARSE_MARKDOWN);

                    } catch (IllegalArgumentException e) {
                        sendService.sendMessageToThread(ctx, "❌ " + e.getMessage() + "\n\nИспользуйте /schedulehelp для справки");
                    } catch (Exception e) {
                        log.error("Error creating schedule", e);
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при создании расписания: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability scheduleHelpAbility() {
        return Ability.builder()
                .name("schedulehelp")
                .info("Показать справку по созданию расписаний")
                .locality(GROUP)
                .privacy(PUBLIC)
                .action(this::sendScheduleHelp)
                .build();
    }

    public Ability listSchedulesAbility() {
        return Ability.builder()
                .name("listschedules")
                .info("Показать активные расписания для этого чата")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> {
                    List<ScheduledPost> schedules = scheduledPostService.getActiveSchedulesForChat(ctx.chatId());

                    if (schedules.isEmpty()) {
                        sendService.sendMessageToThread(ctx, "📭 В этом чате нет активных расписаний");
                        return;
                    }

                    StringBuilder response = new StringBuilder("📅 Активные расписания:\n\n");

                    for (ScheduledPost schedule : schedules) {
                        response.append("🆔 ID: ").append(schedule.getId()).append("\n")
                                .append("📋 Группа: ").append(schedule.getGroupName()).append("\n")
                                .append("📅 Расписание: ").append(schedule.getDescription()).append("\n")
                                .append("💬 Сообщение: ").append(
                                        schedule.getMessageText().length() > 50 ?
                                                schedule.getMessageText().substring(0, 50) + "..." :
                                                schedule.getMessageText()
                                ).append("\n");

                        if (schedule.getImageUrl() != null) {
                            response.append("🖼️ Изображение: есть\n");
                        }

                        if (schedule.getLastSent() != null) {
                            response.append("✅ Последняя отправка: ").append(schedule.getLastSent().toLocalDate()).append("\n");
                        }

                        response.append("\n");
                    }

                    sendService.sendMessageToThread(ctx, response.toString());
                })
                .build();
    }

    public Ability listGroupSchedulesAbility() {
        return Ability.builder()
                .name("listgroupschedules")
                .info("Показать расписания для конкретной группы")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /listgroupschedules <название_группы>");
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Long chatId = ctx.chatId();

                        Optional<ChatGroup> groupOpt = groupManagementService.getGroupByName(chatId, groupName);
                        if (groupOpt.isEmpty()) {
                            sendService.sendMessageToThread(ctx, "❌ Группа '" + groupName + "' не найдена в этом чате");
                            return;
                        }

                        List<ScheduledPost> schedules = scheduledPostService.getActiveSchedulesForGroup(chatId, groupName);

                        if (schedules.isEmpty()) {
                            sendService.sendMessageToThread(ctx, "📭 В группе '" + groupName + "' нет активных расписаний");
                            return;
                        }

                        StringBuilder response = new StringBuilder();
                        response.append("📅 *Активные расписания для группы '").append(groupName).append("':*\n\n");

                        for (ScheduledPost schedule : schedules) {
                            response.append("🆔 *ID:* ").append(schedule.getId()).append("\n")
                                    .append("⏰ *Расписание:* ").append(schedule.getDescription()).append("\n")
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

                            response.append("📊 *Статус:* ").append(schedule.getIsActive() ? "активно ✅" : "неактивно ⏸️").append("\n");

                            response.append("\n");
                        }

                        response.append("💡 *Управление расписаниями:*\n")
                                .append("/toggleschedule <ID> on/off - включить/выключить\n")
                                .append("/deleteschedule <ID> - удалить\n")
                                .append("/scheduleinfo <ID> - подробная информация");

                        sendService.sendMessageToThread(ctx, response.toString(), Constants.PARSE_MARKDOWN);

                    } catch (Exception e) {
                        log.error("Error listing group schedules for chat {}", e.getMessage(), e);
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при получении расписаний группы: " + e.getMessage());
                    }
                })
                .build();
    }

    public Ability toggleScheduleAbility() {
        return Ability.builder()
                .name("toggleschedule")
                .info("Включить/выключить расписание")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(2)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 2) {
                        sendService.sendMessageToThread(ctx, "Использование: /toggleschedule <ID расписания> <on|off>");
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);
                        boolean isActive = "on".equalsIgnoreCase(args[1]);

                        scheduledPostService.toggleSchedule(scheduleId, isActive);

                        sendService.sendMessageToThread(
                                ctx, 
                                isActive ? "✅ Расписание включено" : "⏸️ Расписание отключено"
                        );

                    } catch (NumberFormatException e) {
                        sendService.sendMessageToThread(ctx, "❌ ID расписания должен быть числом");
                    } catch (IllegalArgumentException e) {
                        sendService.sendMessageToThread(ctx, "❌ " + e.getMessage());
                    } catch (Exception e) {
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при изменении расписания");
                    }
                })
                .build();
    }

    public Ability deleteScheduleAbility() {
        return Ability.builder()
                .name("deleteschedule")
                .info("Удалить расписание")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /deleteschedule <ID расписания>");
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);

                        scheduledPostService.deleteSchedule(scheduleId);

                        sendService.sendMessageToThread(ctx, "🗑️ Расписание удалено");

                    } catch (NumberFormatException e) {
                        sendService.sendMessageToThread(ctx, "❌ ID расписания должен быть числом");
                    } catch (IllegalArgumentException e) {
                        sendService.sendMessageToThread(ctx, "❌ " + e.getMessage());
                    } catch (Exception e) {
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при удалении расписания");
                    }
                })
                .build();
    }

    public Ability scheduleInfoAbility() {
        return Ability.builder()
                .name("scheduleinfo")
                .info("Получить подробную информацию о расписании")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        sendService.sendMessageToThread(ctx, "Использование: /scheduleinfo <ID расписания>");
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);
                        Optional<ScheduledPost> scheduleOpt = scheduledPostService.findById(scheduleId);

                        if (scheduleOpt.isPresent()) {
                            ScheduledPost schedule = scheduleOpt.get();

                            StringBuilder response = new StringBuilder();
                            response.append("📊 *Информация о расписании:*\n\n")
                                    .append("🆔 *ID:* ").append(schedule.getId()).append("\n")
                                    .append("📋 *Группа:* ").append(schedule.getGroupName()).append("\n")
                                    .append("📅 *Описание:* ").append(schedule.getDescription()).append("\n")
                                    .append("⚙️ *Cron выражение:* `").append(schedule.getCronExpression()).append("`\n")
                                    .append("💬 *Сообщение:* ").append(schedule.getMessageText()).append("\n")
                                    .append("🖼️ *Изображение:* ").append(schedule.getImageUrl() != null ? "есть" : "нет").append("\n")
                                    .append("📊 *Статус:* ").append(schedule.getIsActive() ? "активно" : "неактивно").append("\n")
                                    .append("👤 *Создано:* ").append(schedule.getCreatedBy()).append("\n")
                                    .append("📅 *Создано:* ").append(schedule.getCreatedAt().toLocalDate()).append("\n");

                            if (schedule.getLastSent() != null) {
                                response.append("✅ *Последняя отправка:* ").append(schedule.getLastSent()).append("\n");
                            }

                            sendService.sendMessageToThread(ctx, response.toString(), Constants.PARSE_MARKDOWN);
                        } else {
                            sendService.sendMessageToThread(ctx, "❌ Расписание с ID " + scheduleId + " не найдено");
                        }

                    } catch (NumberFormatException e) {
                        sendService.sendMessageToThread(ctx, "❌ ID расписания должен быть числом");
                    } catch (Exception e) {
                        sendService.sendMessageToThread(ctx, "❌ Ошибка при получении информации о расписании");
                    }
                })
                .build();
    }

    private void sendScheduleHelp(MessageContext ctx) {
        String helpText = """
            📅 *Создание расписаний*
            
            *Использование:*
            `/createschedule <группа> <расписание> <сообщение>`
            
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
            `/createschedule Тест 09:00 Доброе утро!`
            `/createschedule Созвон пн,ср,пт 10:30 Время созвона!`
            `/createschedule Отчет 1 09:00 Ежемесячный отчет`
            `/listgroupschedules Тест` - показать расписания группы "Тест"
            
            *Для URL изображения добавьте его в конце*
            """;

        sendService.sendMessageToThread(ctx, helpText, Constants.PARSE_MARKDOWN);
    }
}