package ru.kodrul.bot.abilities;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.GroupDistributeService;
import ru.kodrul.bot.utils.Constants;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class GroupDistributeAbility implements AbilityExtension {

    private final AbilityBot extensionAbility;
    private final GroupDistributeService groupDistributeService;

    public Ability distributeAbility() {
        return Ability
                .builder()
                .name("distribute")
                .info(Constants.DISTRIBUTE_GUIDE)
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> groupDistributeService.replayDistribute(messageContext, extensionAbility.silent()))
                .build();
    }
}
