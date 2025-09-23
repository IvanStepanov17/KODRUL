package ru.kodrul.bot.abilities.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatMemberAbility implements AbilityExtension {

    private final AbilityBot extensionAbility;

    public Ability getChatMembers() {
        return Ability.builder()
                .name("members")
                .info("Получить информацию об участниках чата")
                .locality(Locality.GROUP)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    String chatId = ctx.chatId().toString();
                    GetChatMemberCount countRequest = new GetChatMemberCount();
                    countRequest.setChatId(chatId);
                    Optional<Integer> memberCount = extensionAbility.silent().execute(countRequest);
                    memberCount.ifPresentOrElse(
                            count -> extensionAbility.silent().send("Количество участников: " + count, ctx.chatId()),
                            () -> extensionAbility.silent().send("Ошибка при получении количества участников", ctx.chatId()));

                })
                .build();
    }
}
