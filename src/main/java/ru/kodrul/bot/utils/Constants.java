package ru.kodrul.bot.utils;

import java.util.List;

public class Constants {

    public static final int MAX_MESSAGE_LENGTH = 4096;
    public static final int MAX_MENTIONS_PER_MESSAGE = 40;
    public static final int MAX_CAPTION_LENGTH = 1024;

    public static final List<String> ROULETTE_SUCCESSFUL = List.of(
            "Осечка. %s может спокойно выдохнуть.",
            "Сухой щелчок возвестил о том, что %s выживает.",
            "Осечка. Сегодня у %s счастливый день.",
            "Щелчок. %s избегает выстрела.");

    public static final List<String> ROULETTE_FAILED = List.of(
            "В звенящей тишине раздался оглушительный выстрел! Для %s игра окончена.",
            "Бах! Ангел-хранитель %s сегодня взял выходной. Игра окончена.");

    public static final String ROULETTE_START = "Начинаем русскую рулетку! Представляю вам список наших бесстрашных " +
            "участников: %s. Количество патронов в барабане револьвера: %s";
}
