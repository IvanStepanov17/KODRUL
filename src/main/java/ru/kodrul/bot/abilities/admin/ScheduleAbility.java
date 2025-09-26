package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.entity.ScheduledPost;
import ru.kodrul.bot.services.ScheduledPostService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAbility implements AbilityExtension {

    @Lazy
    private final AbilityBot abilityBot;
    private final ScheduledPostService scheduledPostService;

    public Ability createScheduleAbility() {
        return Ability.builder()
                .name("createschedule")
                .info("Создать расписание - укажите группу, время, а затем сообщение")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(ctx -> {
                    String fullText = ctx.update().getMessage().getText();
                    String[] parts = fullText.split("\\s+", 4); // Разбиваем на 4 части максимум

                    if (parts.length < 4) {
                        abilityBot.silent().send(
                                "Использование: /createschedule <группа> <время> <сообщение>\n\n" +
                                        "Пример: /createschedule Тест2 09:00 Доброе утро, команда!\n" +
                                        "Для URL изображения добавьте его в конце через пробел",
                                ctx.chatId()
                        );
                        return;
                    }

                    try {
                        String groupName = parts[1];
                        String timeStr = parts[2];
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

                        LocalTime scheduledTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));

                        ScheduledPost schedule = scheduledPostService.createSchedule(
                                ctx.chatId(), groupName, scheduledTime, messageText, imageUrl, ctx.user().getId()
                        );

                        String response = String.format(
                                "✅ Расписание создано!\n\n" +
                                        "📋 Группа: %s\n" +
                                        "⏰ Время: %s\n" +
                                        "💬 Сообщение: %s\n" +
                                        "%s" +
                                        "🆔 ID: %d",
                                groupName,
                                scheduledTime,
                                messageText,
                                imageUrl != null ? "🖼️ Изображение: " + imageUrl + "\n" : "",
                                schedule.getId()
                        );

                        abilityBot.silent().send(response, ctx.chatId());

                    } catch (DateTimeParseException e) {
                        abilityBot.silent().send("❌ Неверный формат времени. Используйте HH:mm", ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка: " + e.getMessage(), ctx.chatId());
                    }
                })
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
                                .append("⏰ Время: ").append(schedule.getScheduledTime()).append("\n")
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

    public Ability testScheduleAbility() {
        return Ability.builder()
                .name("testschedule")
                .info("Тестовая отправка сообщения по расписанию")
                .locality(GROUP)
                .privacy(ADMIN)
                .input(1)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 1) {
                        abilityBot.silent().send("Использование: /testschedule <ID расписания>", ctx.chatId());
                        return;
                    }

                    try {
                        Long scheduleId = Long.parseLong(args[0]);

                        abilityBot.silent().send("🔧 Тестовая функция в разработке. Используйте обычное расписание.", ctx.chatId());

                    } catch (NumberFormatException e) {
                        abilityBot.silent().send("❌ ID расписания должен быть числом", ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при тестировании расписания", ctx.chatId());
                    }
                })
                .build();
    }
}