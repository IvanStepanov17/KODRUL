package ru.kodrul.bot.abilities;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.RouletteService;
import ru.kodrul.bot.utils.Constants;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class RouletteAbility implements AbilityExtension {

    private final AbilityBot extensionAbility;
    private final RouletteService rouletteService;

    public Ability rouletteAbility() {
        return Ability
                .builder()
                .name("roulette")
                .info(Constants.ROULETTE_GUIDE)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> rouletteService.replyRussianRoulette(messageContext, extensionAbility.silent()))
                .build();
    }
}
