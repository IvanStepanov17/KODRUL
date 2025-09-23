package ru.kodrul.bot.utils;

import java.util.List;

public class Constants {

    public static final List<String> RANDOMIZE_SUCCESSFUL = List.of(
            "Осечка. %s может спокойно выдохнуть.",
            "Сухой щелчок возвестил о том, что %s выживает.",
            "Осечка. Сегодня у %s счастливый день.",
            "Щелчок. %s избегает выстрела.");

    public static final List<String> RANDOMIZE_FAILED = List.of(
            "В звенящей тишине раздался оглушительный выстрел! Для %s игра окончена.",
            "Бах! Ангел-хранитель %s сегодня взял выходной. Игра окончена.");

    public static final String ROULETTE_START = "Начинаем русскую рулетку! Представляю вам список наших бесстрашных " +
            "участников: %s. Количество патронов в барабане револьвера: %s";

    public static final String ROULETTE_GUIDE = "Для запуска русской рулетки тэгните " +
            "участников и укажите количество патронов в револьвере (обойма по умолчанию равна 6). " +
            "Пример: /roulette @Player1 @Player2 1 (учтите, что патронов не может быть больше или равно количеству участников.)";

    public static final String RANDOMIZE_GUIDE = "Для выбора по одному участнику по каждому направлению " +
            "укажите по каким направлениям требуется выбор " +
            "Пример: /randomize qa front back ann";

    public static final String DISTRIBUTE_GUIDE = "Для того чтобы разделить участников на 4 группы " +
            "тэгните участников " +
            "Пример: /distribute @Player1 @Player2 @Player3 @Player4 (учтите, что нужно не менее 4ёх участников)";

    public static final String DISTRIBUTE_START = "Начинаю распределение участников по группам. Готовы ли вы? " +
            "Дрожат ли ваши коленки? Хотите узнать кто будет прикрывать вашу спину на игре 100 к 1?!";

}
