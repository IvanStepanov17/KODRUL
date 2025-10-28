package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.common.CommonAbilityHelper;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.parser.CommandParser;
import ru.kodrul.bot.pojo.CommandArguments;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.ScheduledService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

import java.util.List;
import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAbility implements AbilityExtension {

    private final ScheduledService scheduledService;
    private final GroupManagementService groupManagementService;
    private final SendService sendService;
    private final CommandParser commandParser;
    private final CommonAbilityHelper commonAbilityHelper;

    public Ability createScheduleAbility() {
        return Ability.builder()
                .name("createschedule")
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
                        CommandArguments args = commandParser.parseCommandWithQuotes(fullText);

                        if (args.getGroupName() == null || args.getSchedule() == null || args.getMessage() == null) {
                            sendScheduleHelp(ctx);
                            return;
                        }

                        String groupName = args.getGroupName();
                        String scheduleInput = args.getSchedule();
                        String messageText = args.getMessage();
                        String imageUrl = args.getImageUrl();

                        Optional<ChatGroup> groupOpt = groupManagementService.getGroupByName(ctx.chatId(), groupName);
                        if (groupOpt.isEmpty()) {
                            sendService.sendMessageToThread(ctx,
                                    "❌ Группа '" + groupName + "' не найдена в указанном чате\n\n" +
                                            "Сначала создайте группу командой:\n" +
                                            "/creategroup " + groupName + " \"Описание группы\"");
                            return;
                        }

                        String chatTitle = commonAbilityHelper.getChatTitle(ctx.chatId());
                        Integer messageThreadId = ctx.update().getMessage().getMessageThreadId();

                        ScheduledPost schedule = scheduledService.createSchedule(
                                ctx.chatId(), messageThreadId, groupName, scheduleInput, messageText, imageUrl, ctx.user().getId()
                        );

                        String response = String.format(
                                """
                                        ✅ *Расписание создано!*

                                        📋 *Группа:* %s
                                        💬 *Чат:* %s
                                        📅 *Расписание:* %s
                                        💬 *Сообщение:* %s
                                        %s
                                        👤 Создано: %s (ID: %d)
                                        🆔 *ID:* %d
                                """,
                                groupName,
                                chatTitle,
                                schedule.getDescription(),
                                messageText,
                                imageUrl != null ? "🖼️ *Изображение:* есть\n" : "",
                                ctx.user().getFirstName(),
                                ctx.user().getId(),
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
                .locality(GROUP)
                .privacy(PUBLIC)
                .action(this::sendScheduleHelp)
                .build();
    }

    public Ability listSchedulesAbility() {
        return Ability.builder()
                .name("listschedules")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> {
                    List<ScheduledPost> schedules = scheduledService.getActiveSchedulesForChat(ctx.chatId());

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

                        List<ScheduledPost> schedules = scheduledService.getActiveSchedulesForGroup(chatId, groupName);

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

                        scheduledService.toggleSchedule(scheduleId, isActive);

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

                        scheduledService.deleteSchedule(scheduleId);

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
                        Optional<ScheduledPost> scheduleOpt = scheduledService.findById(scheduleId);

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
            📅 *Использование:* /createschedule <группа> "<расписание>" <сообщение>
            
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
            /createschedule -100123456789 Тест "09:00" "Доброе утро!"
            /createschedule -100123456789 Созвон "еженедельно пн,ср,пт 10:30" "Время созвона!"
            /createschedule -100123456789 Отчет "ежемесячно 1 09:00" "Ежемесячный отчет"
            /createschedule -100123456789 Обед "0 0 12 * * ?" "Время обеда!"
            
            *Для URL изображения добавьте его в конце:*
            /createschedule -100123456789 Тест "09:00" "Доброе утро!" https://example.com/image.jpg
            """;

        sendService.sendMessageToThread(ctx, helpText, Constants.PARSE_MARKDOWN);
    }
}