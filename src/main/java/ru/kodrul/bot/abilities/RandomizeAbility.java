package ru.kodrul.bot.abilities;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.RandomizeService;
import ru.kodrul.bot.utils.Constants;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class RandomizeAbility implements AbilityExtension {

    private final AbilityBot extensionAbility;
    private final RandomizeService randomizeService;

    public Ability randomizeAbility() {
        return Ability
                .builder()
                .name("randomize")
                .info(Constants.RANDOMIZE_GUIDE)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> randomizeService.replayRandomize(messageContext, extensionAbility.silent()))
                .build();
    }
}
