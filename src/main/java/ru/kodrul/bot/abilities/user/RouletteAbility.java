package ru.kodrul.bot.abilities.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.RouletteService;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class RouletteAbility implements AbilityExtension {

    @Lazy
    private final AbilityBot abilityBot;
    private final RouletteService rouletteService;

    public Ability rouletteAbility() {
        return Ability
                .builder()
                .name("roulette")
                .info("Русская рулетка. Использование: /roulette <тэгните участников через пробел> <количество патронов>")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> rouletteService.replyRussianRoulette(messageContext, abilityBot.silent()))
                .build();
    }
}
