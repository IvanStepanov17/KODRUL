package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.common.CommonAbilityHelper;
import ru.kodrul.bot.services.SendService;

import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemberAbility implements AbilityExtension {

    private final SendService sendService;
    private final CommonAbilityHelper commonAbilityHelper;

    public Ability getChatMembers() {
        return Ability.builder()
                .name("members")
                .info("Получить информацию об участниках чата")
                .locality(Locality.GROUP)
                .privacy(PUBLIC)
                .action(ctx -> {
                    Optional<Integer> memberCount = commonAbilityHelper.getMemberCount(ctx);
                    memberCount.ifPresentOrElse(
                            count -> sendService.sendMessageToThread(ctx, "Количество участников: " + count),
                            () -> sendService.sendMessageToThread(ctx, "Ошибка при получении количества участников"));

                })
                .build();
    }

    public Ability chatMembersAbility() {
        return Ability.builder()
                .name("chatmembers")
                .info("Получить список участников чата")
                .locality(GROUP)
                .privacy(ADMIN)
                .action(this::getChatMembers)
                .build();
    }

    private void getChatMembers(MessageContext ctx) {
        try {
            // TODO реализовать список участников чата
            var user = ctx.user();
            String membersInfo = String.format("""
                            👥 Информация о чате:

                            👤 Вы: %s %s (ID: %d)

                            Для получения полного списка участников используйте синхронизацию""",
                    user.getFirstName(), user.getLastName(), user.getId());

            sendService.sendMessageToThread(ctx, membersInfo);

        } catch (Exception e) {
            sendService.sendMessageToThread(ctx, "❌ Ошибка при получении информации о чате");
        }
    }
}
