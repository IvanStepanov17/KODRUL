package ru.kodrul.bot.abilities.hidden;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.AuthorizationService;
import ru.kodrul.bot.services.SendService;
import ru.kodrul.bot.utils.Constants;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class HiddenHelperAbility implements AbilityExtension {

    private final AuthorizationService authorizationService;
    private final SendService sendService;

    public Ability hiddenHelpAbility() {
        return Ability.builder()
                .name("helphidden")
                .info("Показать список доступных команд")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> {
                    Long userId = ctx.user().getId();
                    if (!authorizationService.isTrustedUser(userId)) {
                        sendService.sendToUser(userId, "❌ У вас нет прав для использования этой команды");
                        return;
                    }

                    String helpText = """
                        🕵️ *Скрытые команды для доверенных пользователей*

                        *Управление группами:*
                        • `/creategrouphidden <ID чата> <название> [описание]` - Создать группу для указанного чата
                        • `/addmembershidden <группа> <тэгните участников через пробел>` - Добавить участников в группу
                        • `/removemembershidden <группа> <тэгните участников через пробел>` - Удалить участников из группы
                        • `/addtrusteduser <ID пользователя> <ключ>` - Добавить доверенного пользователя (только с административным ключом)
                        • `/listtrustedusers` - Показать количество доверенных пользователей

                        *Управление расписаниями:*
                        • `/createschedulehidden <ID чата> <группа> <расписание> <сообщение>` - Создать расписание для указанного чата
                        • `/listscheduleshidden <ID чата>` - Показать расписания для указанного чата
                        • `/toggleschedulehidden <ID расписания> on/off` - Включить/выключить расписание
                        • `/deleteschedulehidden <ID расписания>` - Удалить указанное расписание
                        • `/scheduleinfohidden <ID расписания>` - Получить подробную информацию о расписании

                        *Форматы расписания:*
                        • `09:00` - ежедневно в 9:00
                        • `пн,ср,пт 09:00` - по пн, ср, пт в 9:00
                        • `1,15 09:00` - 1 и 15 числа каждого месяца
                        • `0 0 9 * * ?` - cron выражение

                        *Примеры:*
                        /creategrouphidden -100123456789 Тест "Тестовая группа"
                        /createschedulehidden -100123456789 Тест 09:00 "Доброе утро!"
                        /listscheduleshidden -100123456789

                        💡 *Примечание:* Бот должен быть участником целевого чата!
                        """;

                    sendService.sendToUser(userId, helpText, Constants.PARSE_MARKDOWN);
                })
                .build();
    }
}