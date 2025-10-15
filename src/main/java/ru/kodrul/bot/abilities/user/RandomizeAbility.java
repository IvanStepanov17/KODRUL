package ru.kodrul.bot.abilities.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.util.AbilityExtension;
import ru.kodrul.bot.services.RandomizeService;
import ru.kodrul.bot.utils.EscapeHelper;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
@RequiredArgsConstructor
public class RandomizeAbility implements AbilityExtension {

    @Lazy
    private final AbilityBot abilityBot;
    private final RandomizeService randomizeService;

    public Ability randomizeAbility() {
        return Ability
                .builder()
                .name("randomize")
                .info("Рандомайзер. Использование: /randomize <Наименование групп через пробел>")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> randomizeService.replayRandomize(ctx, abilityBot.silent()))
                .build();
    }

    public Ability randomizeMultipleAbility() {
        return Ability
                .builder()
                .name("randomizemulti")
                .info("Выбрать несколько случайных участников из группы. Использование: /randomizemulti <группа> <количество>")
                .locality(ALL)
                .privacy(PUBLIC)
                .input(2)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 2) {
                        abilityBot.silent().send("Использование: /randomizemulti <группа> <количество>", ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = args[0];
                        int count = Integer.parseInt(args[1]);
                        randomizeService.randomizeMultipleFromGroup(ctx, abilityBot.silent(), groupName, count);
                    } catch (NumberFormatException e) {
                        abilityBot.silent().send("❌ Количество должно быть числом", ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при выполнении команды", ctx.chatId());
                    }
                })
                .build();
    }

    public Ability distributeTeamsAbility() {
        return Ability
                .builder()
                .name("distributeteams")
                .info("Распределить участников группы на команды.")
                .locality(ALL)
                .privacy(PUBLIC)
                .input(2)
                .action(ctx -> {
                    String[] args = ctx.arguments();
                    if (args.length < 2) {
                        abilityBot.silent().send("Использование: /distributeteams <группа> <количество_команд>", ctx.chatId());
                        return;
                    }

                    try {
                        String groupName = args[0];
                        int teamCount = Integer.parseInt(args[1]);
                        randomizeService.distributeGroupToTeams(ctx, abilityBot.silent(), EscapeHelper.escapeMarkdownV2(groupName), teamCount);
                    } catch (NumberFormatException e) {
                        abilityBot.silent().send("❌ Количество команд должно быть числом", ctx.chatId());
                    } catch (Exception e) {
                        abilityBot.silent().send("❌ Ошибка при распределении по командам", ctx.chatId());
                    }
                })
                .build();
    }
}