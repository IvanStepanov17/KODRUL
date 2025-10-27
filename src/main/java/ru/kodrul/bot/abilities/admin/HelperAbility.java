package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.AuthorizationService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

@Slf4j
@Component
@RequiredArgsConstructor
public class HelperAbility implements AbilityExtension {

    private final AuthorizationService authorizationService;
    private final SendService sendService;

    public Ability reportCommands() {
        return Ability.builder()
                .name("commands")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    try {
                        Long userId = ctx.user().getId();

                        StringBuilder commands = new StringBuilder();
                        commands.append("🤖 *Доступные команды:*\n\n");

                        commands.append("*👥 Управление группами:*\n");
                        commands.append("• `/creategroup <название> [описание]` - Создать новую группу участников\n");
                        commands.append("• `/deletegroup <группа>` - Удалить группу участников\n");
                        commands.append("• `/listgroups` - Показать все группы в чате\n");
                        commands.append("• `/groupinfo <группа>` - Получить подробную информацию о группе и её участниках\n");
                        commands.append("• `/addmembers <группа> <тэгните участников через пробел>` - Добавить участников в группу\n");
                        commands.append("• `/removemembers <группа> <тэгните участников через пробел>` - Удалить участников из группы\n");
                        commands.append("• `/groupmembers <группа>` - Получить список участников группы\n");
                        commands.append("• `/groupssummary` - Краткая сводка по всем группам в чате\n");
                        commands.append("• `/tag <группа>` - Тэгнуть участников группы\n\n");

                        commands.append("*📅 Расписания:*\n");
                        commands.append("• `/createschedule <группа> <расписание> <сообщение>` - Создать расписание для группы\n");
                        commands.append("• `/listschedules` - Показать активные расписания для этого чата\n");
                        commands.append("• `/listgroupschedules <название_группы>` - Показать расписания для группы\n");
                        commands.append("• `/toggleschedule <ID расписания> on/off` - Включить/выключить расписание\n");
                        commands.append("• `/deleteschedule <ID расписания>` - Удалить расписание\n");
                        commands.append("• `/scheduleinfo <ID расписания>` - Получить подробную информацию о расписании\n");
                        commands.append("• `/schedulehelp` - Показать справку по созданию расписаний\n\n");

                        commands.append("*🎲 Развлечения:*\n");
                        commands.append("• `/roulette <тэгните участников через пробел> <количество патронов>` - Русская рулетка\n");
                        commands.append("• `/randomize <наименование групп через пробел>` - Выбор случайного участника из группы/групп\n");
                        commands.append("• `/randomizemulti <группа> <количество>` - Выбрать несколько случайных участников из группы.\n");
                        commands.append("• `/distributeteams <группа> <количество_команд>` - Распределить участников группы на команды.\n\n");

                        commands.append("*👑 Администрирование:*\n");
                        commands.append("• `/addusers <тэгните пользователей через пробел>` - Добавить пользователей в базу данных по их username\n");
                        commands.append("• `/members` - Получить информацию об участниках чата\n");
                        commands.append("• `/chatmembers` - Информация о чате (пока в разработке)\n\n");

                        if (authorizationService.isTrustedUser(userId)) {
                            commands.append("*🕵️ Скрытые команды (только для доверенных пользователей):*\n");
                            commands.append("• /helphidden - Справка по скрытым командам\n");
                        }

                        sendService.sendMessageToThread(ctx, commands.toString(), Constants.PARSE_MARKDOWN);

                    } catch (Exception e) {
                        log.error("Error in custom commands", e);
                        sendService.sendMessageToThread(ctx, "Произошла ошибка при получении списка команд");
                    }
                })
                .build();
    }
}