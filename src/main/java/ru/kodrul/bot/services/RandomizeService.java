package ru.kodrul.bot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.SilentSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
public class RandomizeService {

    private List<String> QA_TEAM = new ArrayList<>(Stream.of(
            "@maks_233",
            "@Gulnara_Abdrakhmanova",
            "@Miketoomike"
    ).toList());

    private List<String> FRONT_TEAM = new ArrayList<>(Stream.of(
            "@Hinoro",
            "@DzibaMaksim"
    ).toList());

    private List<String> BACK_TEAM = new ArrayList<>(Stream.of(
            "@coftsonyk",
            "@alexandr_burov",
            "@Widowmak",
            "@sdsliu"
    ).toList());

    private List<String> ANALYTIC_TEAM = new ArrayList<>(Stream.of(
            "@OSeySan",
            "@Romka_KRD",
            "@portnayan"
    ).toList());

    private final Map<String, List<String>> teamMap = Map.of(
            "qa",QA_TEAM,
            "front", FRONT_TEAM,
            "back", BACK_TEAM,
            "ann", ANALYTIC_TEAM
    );

    public void replayRandomize(MessageContext context, SilentSender sender) {

        StringBuilder builder = new StringBuilder().append("Представляю Вам список сегодняшних мучеников! \n");

        List<String> arguments = new ArrayList<>(Arrays.asList(context.arguments()));

        arguments
                .forEach(arg -> {
                    if (teamMap.containsKey(arg)) {
                        shuffleTeam(arg);
                        builder.append(String.format("По направлению %s: %s \n", arg, teamMap.get(arg).get(0)));
                    }
                });

        if (builder.length() < 50) {
            sender.send("Укажите верный аргумент для выбора жертвы", context.chatId());
        } else {
            sender.send(builder.toString(), context.chatId());
        }
    }

    private void shuffleTeam(String tagTeam) {
        Collections.shuffle(teamMap.get(tagTeam));
    }
}