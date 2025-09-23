package ru.kodrul.bot.abilities;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.utils.Constants;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class HelperAbility implements AbilityExtension {

    private final AbilityBot extensionAbility;

    public Ability helpAbility() {
        return Ability
                .builder()
                .name("help")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> extensionAbility.silent().send(Constants.ROULETTE_GUIDE, messageContext.chatId()))
                .build();
    }
}
