package ru.kodrul.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import ru.kodrul.bot.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteService {

    @Transactional
    public void replyRussianRoulette(MessageContext context, SilentSender sender) {

        var users = context.bot().userIds();
        var players = new ArrayList<String>();
        var bulletCount = new ArrayList<Integer>();

        Arrays.stream(context.arguments())
                .forEach(arg -> {
//                    if (arg.startsWith("@") && !users.containsKey(arg.substring(1).toLowerCase())) {
//                        sendMessage(context.chatId(), "В списке участников группы не найден пользователь: " + arg);
//                        return;
                    if (arg.startsWith("@")) {
                        players.add(arg);
                    } else if (!arg.startsWith("@") && NumberUtils.isParsable(arg)) {
                        bulletCount.add(Integer.parseInt(arg));
                    }
                });

        if (bulletCount.isEmpty() || players.isEmpty()) {
            sender.send("Проверьте правильность ввода команды.", context.chatId());
            return;
        }

        Collections.shuffle(players);

        try {
            playRussianRoulette(players, bulletCount.get(0), context.chatId(), sender);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void playRussianRoulette(ArrayList<String> players, Integer bullets, Long chatId, SilentSender sender) throws InterruptedException {
        sender.send(String.format(Constants.ROULETTE_START, players.toString(), bullets.toString()), chatId);
        Thread.sleep(2000L);

        if (bullets >= players.size()) {
            sender.send("Количество патронов не может быть больше или равно количеству участников.", chatId);
            return;
        }

        List<String> chamber = new ArrayList<>(Collections.nCopies(players.size(), "пусто"));
//        List<String> chamber = new ArrayList<>(Collections.nCopies(6, "пусто"));
        Random random = new Random();

        // Распределяем заряженные патроны
        for (int i = 0; i < bullets; i++) {
            int position;
            do {
                position = random.nextInt(/*players.size()*/chamber.size());
            } while (chamber.get(position).equals("заряжено"));
            chamber.set(position, "заряжено");
        }

        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            if (chamber.get(i).equals("заряжено")) {
                sender.send(String.format(Constants.RANDOMIZE_FAILED.get(new Random().nextInt(Constants.RANDOMIZE_FAILED.size())), player), chatId);
                if (bullets > 1) {
                    Thread.sleep(1000);
                    sender.send("Состояние обоймы: " + chamber, chatId);
                }
                return;
            } else {
                sender.send(String.format(Constants.RANDOMIZE_SUCCESSFUL.get(new Random().nextInt(Constants.RANDOMIZE_SUCCESSFUL.size())), player), chatId);
            }
            Thread.sleep(1500);
        }

        sender.send("В этот раз всем повезло. Даже уборщице, ведь ей не придётся соскребать мозги со стен... Состояние обоймы: " + chamber, chatId);
    }

}
