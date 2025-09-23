package ru.kodrul.bot.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import ru.kodrul.bot.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class GroupDistributeService {

    private List<String> groupNames = Stream.of(
            "Золотые Труселя",
            "Звёзды порнохаба",
            "Клуб фанатов Сюткина",
            "Прокурорский генг",
            "Сферические кони в вакууме",
            "Серьезное товарищество",
            "Одинокие милфы",
            "тРУЛшные промоКОДы",
            "Садо-мазо-код",
            "КОДовый ПатРУЛь",
            "Жертвы импортозамещения",
            "Новый ВордЪ",
            "Не тонущие в воде",
            "Трели Кулика",
            "Про ПроПРо",
            "КодРулители",
            "КОД бРУЛе",
            "Горящие пуканы",
            "РовноКод",
            "Коты Облизывают Диван",
            "Львы киркпинара",
            "цирк у КОДов",
            "Боевые кодики",
            "Струя бобра",
            "КамКод",
            "Кровавые шлюхи в яме",
            "Runtime Terror",
            "50 оттенков багов",
            "Кодеры в тумане",
            "Кладовая деда"
    ).collect(Collectors.toList());

    public void replayDistribute(MessageContext context, SilentSender sender) {

        var members = new ArrayList<String>();
        var groupCount = new ArrayList<Integer>();

        Arrays.stream(context.arguments())
                .forEach(arg -> {
//                    if (arg.startsWith("@") && !users.containsKey(arg.substring(1).toLowerCase())) {
//                        sendMessage(context.chatId(), "В списке участников группы не найден пользователь: " + arg);
//                        return;
                    if (arg.startsWith("@")) {
                        members.add(arg);
                    } else if (!arg.startsWith("@") && NumberUtils.isParsable(arg)) {
                        groupCount.add(Integer.parseInt(arg));
                    }
                });

        if (groupCount.isEmpty() || groupCount.get(0) < 2) {
            sender.send("Количество групп не может быть менее 2.", context.chatId());
            return;
        }

        // Перемешиваем участников перед распределением
        Collections.shuffle(members);
        List<List<String>> groups = distributeMembersByGroups(members, groupCount.get(0));

        sender.send(Constants.DISTRIBUTE_START, context.chatId());
        try {
            Thread.sleep(5000L);
            sender.send("Ну чтож ещё пара секунд и....", context.chatId());
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Collections.shuffle(groupNames);
        // Формирование ответа
        StringBuilder response = new StringBuilder("Распределение участников завершено!\n");
        for (int i = 0; i < groups.size(); i++) {
            response.append(groupNames.get(i)).append(": ").append(groups.get(i)).append("\n");
        }

        try {
            sender.send(response.toString(), context.chatId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<List<String>> distributeMembersByGroups(List<String> members, int numberOfGroups) {
        List<List<String>> groups = new ArrayList<>();

        // Инициализация списков для групп
        for (int i = 0; i < numberOfGroups; i++) {
            groups.add(new ArrayList<>());
        }

        // Распределение участников по группам
        for (int i = 0; i < members.size(); i++) {
            groups.get(i % numberOfGroups).add(members.get(i));
        }

        return groups;
    }
}
