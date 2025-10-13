package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.ChatGroup;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.services.GroupManagementService;
import ru.kodrul.bot.services.ScheduledPostService;

import java.util.List;
import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAbility implements AbilityExtension {

    @Lazy
    private final AbilityBot abilityBot;
    private final ScheduledPostService scheduledPostService;
    private final GroupManagementService groupManagementService;

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
                        sendScheduleHelp(ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = parts[1];
                        String scheduleInput = parts[2];
                        String restOfText = parts[3];

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

                        abilityBot.silent().sendMd(response, ctx.chatId());

                    } catch (IllegalArgumentException e) {
                        abilityBot.silent().send("❌ " + e.getMessage() + "\n\nИспользуйте /schedulehelp для справки", ctx.chatId());
                    } catch (Exception e) {
                        log.error("Error creating schedule", e);
                        abilityBot.silent().send("❌ Ошибка при создании расписания: " + e.getMessage(), ctx.chatId());
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
                .action(ctx -> sendScheduleHelp(ctx.chatId()))
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
                        abilityBot.silent().send("📭 В этом чате нет активных расписаний", ctx.chatId());
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

                    abilityBot.silent().send(response.toString(), ctx.chatId());
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
                        abilityBot.silent().send("Использование: /listgroupschedules <название_группы>", ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = args[0];
                        Long chatId = ctx.chatId();

                        Optional<ChatGroup> groupOpt = groupManagementService.getGroupByName(chatId, groupName);
                        if (groupOpt.isEmpty()) {
                            abilityBot.silent().send("❌ Группа '" + groupName + "' не найдена в этом чате", ctx.chatId());
                            return;
                        }

                        List<ScheduledPost> schedules = scheduledPostService.getActiveSchedulesForGroup(chatId, groupName);

                        if (schedules.isEmpty()) {
                            abilityBot.silent().send("📭 В группе '" + groupName + "' нет активных расписаний", ctx.chatId());
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

                        abilityBot.silent().sendMd(response.toString(), ctx.chatId());

                    } catch (Exception e) {
                        log.error("Error listing group schedules for chat {}: {}", ctx.chatId(), e.getMessage(), e);
                        abilityBot.silent().send("❌ Ошибка при получении расписаний группы: " + e.getMessage(), ctx.chatId());
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
                        abilityBot.silent().send("Использование: /toggleschedule <ID расписания> <on|off>", ctx.chatId());
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);
                        boolean isActive = "on".equalsIgnoreCase(args[1]);

                        scheduledPostService.toggleSchedule(scheduleId, isActive);

                        abilityBot.silent().send(
                                isActive ? "✅ Расписание включено" : "⏸️ Расписание отключено",
                                ctx.chatId()
                        );

                    } catch (NumberFormatException e) {
                        abilityBot.silent().send("❌ ID расписания должен быть числом", ctx.chatId());
                    } catch (IllegalArgumentException e) {
                        abilityBot.silent().send("❌ " + e.getMessage(), ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при изменении расписания", ctx.chatId());
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
                        abilityBot.silent().send("Использование: /deleteschedule <ID расписания>", ctx.chatId());
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);

                        scheduledPostService.deleteSchedule(scheduleId);

                        abilityBot.silent().send("🗑️ Расписание удалено", ctx.chatId());

                    } catch (NumberFormatException e) {
                        abilityBot.silent().send("❌ ID расписания должен быть числом", ctx.chatId());
                    } catch (IllegalArgumentException e) {
                        abilityBot.silent().send("❌ " + e.getMessage(), ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при удалении расписания", ctx.chatId());
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
                        abilityBot.silent().send("Использование: /scheduleinfo <ID расписания>", ctx.chatId());
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

                            abilityBot.silent().sendMd(response.toString(), ctx.chatId());
                        } else {
                            abilityBot.silent().send("❌ Расписание с ID " + scheduleId + " не найдено", ctx.chatId());
                        }

                    } catch (NumberFormatException e) {
                        abilityBot.silent().send("❌ ID расписания должен быть числом", ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при получении информации о расписании", ctx.chatId());
                    }
                })
                .build();
    }

    private void sendScheduleHelp(Long chatId) {
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
            • `0 0 9 ? * MON,WED,FRI` - по пн, ср, пт в 9:00
            • `0 0 9 1 * ?` - первое число месяца в 9:00
            • `0 0 12 ? * SUN` - каждое воскресенье в 12:00
            
            *Дни недели:* пн, вт, ср, чт, пт, сб, вс
            
            *Другие команды:*
            • `/listschedules` - показать все активные расписания в чате
            • `/listgroupschedules <группа>` - показать расписания для конкретной группы
            • `/scheduleinfo <ID>` - информация о расписании
            • `/toggleschedule <ID> <on|off>` - включить/выключить
            • `/deleteschedule <ID>` - удалить расписание
            
            *Примеры:*
            `/createschedule Тест 09:00 Доброе утро!`
            `/createschedule Созвон пн,ср,пт 10:30 Время созвона!`
            `/createschedule Отчет 1 09:00 Ежемесячный отчет`
            `/listgroupschedules Тест` - показать расписания группы "Тест"
            
            *Для URL изображения добавьте его в конце*
            """;

        abilityBot.silent().sendMd(helpText, chatId);
    }
}