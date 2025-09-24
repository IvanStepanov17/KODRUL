package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;

import java.util.Optional;

import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemberAbility implements AbilityExtension {

    private final AbilityBot abilityBot;

    public Ability getChatMembers() {
        return Ability.builder()
                .name("members")
                .info("Получить информацию об участниках чата")
                .locality(Locality.GROUP)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String chatId = ctx.chatId().toString();
                    GetChatMemberCount countRequest = new GetChatMemberCount();
                    countRequest.setChatId(chatId);
                    Optional<Integer> memberCount = abilityBot.silent().execute(countRequest);
                    memberCount.ifPresentOrElse(
                            count -> abilityBot.silent().send("Количество участников: " + count, ctx.chatId()),
                            () -> abilityBot.silent().send("Ошибка при получении количества участников", ctx.chatId()));

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
            String membersInfo = String.format("👥 Информация о чате:\n\n👤 Вы: %s %s (ID: %d)\n\n" +
                            "Для получения полного списка участников используйте синхронизацию",
                    user.getFirstName(), user.getLastName(), user.getId());

            abilityBot.silent().send(membersInfo, ctx.chatId());

        } catch (Exception e) {
            abilityBot.silent().send("❌ Ошибка при получении информации о чате", ctx.chatId());
        }
    }
}
