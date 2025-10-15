package ru.kodrul.bot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import ru.kodrul.bot.pojo.CronParseResult;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CronService {

    /**
     * Парсит пользовательский ввод в cron-выражение
     */
    public CronParseResult parseCronExpression(String userInput) {
        userInput = userInput.trim().toLowerCase();

        try {
            // Если это уже cron-выражение - проверяем его валидность
            if (isValidCronExpression(userInput)) {
                String description = generateDescription(userInput);
                return new CronParseResult(userInput, description, true);
            }

            // Пытаемся распознать простые форматы
            return parseSimpleFormats(userInput);

        } catch (Exception e) {
            throw new IllegalArgumentException("Неверный формат расписания: " + userInput);
        }
    }

    /**
     * Проверяет валидность cron-выражения
     */
    private boolean isValidCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Парсит простые форматы расписаний
     */
    private CronParseResult parseSimpleFormats(String userInput) {
        // Ежедневно в определенное время: "09:00" или "ежедневно 09:00"
        if (userInput.matches("(ежедневно\\s+)?([0-1]?[0-9]|2[0-3]):[0-5][0-9]")) {
            var time = userInput.replace("ежедневно", "").trim();
            var timeParts = time.split(":");
            var hour = timeParts[0];
            var min = timeParts[1];
            var cron = String.format("0 %s %s * * ?", min, hour);
            var description = "Ежедневно в " + time;
            return new CronParseResult(cron, description, true);
        }

        // Еженедельно в определенные дни: "пн,ср,пт 09:00" или "еженедельно пн,ср,пт 09:00"
        if (userInput.matches("(еженедельно\\s+)?([пнвтсрчтптсбвс1-7,\\s]+)\\s+([0-1]?[0-9]|2[0-3]):[0-5][0-9]")) {
            return parseWeeklyFormat(userInput);
        }

        // Ежемесячно в определенные дни: "1,15 09:00" или "ежемесячно 1,15 09:00"
        if (userInput.matches("(ежемесячно\\s+)?[0-9,\\s]+\\s+([0-1]?[0-9]|2[0-3]):[0-5][0-9]")) {
            return parseMonthlyFormat(userInput);
        }

        throw new IllegalArgumentException("Не удалось распознать формат расписания: " + userInput);
    }

    private CronParseResult parseWeeklyFormat(String userInput) {
        String cleaned = userInput.replace("еженедельно", "").trim();
        String[] parts = cleaned.split("\\s+");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Неверный формат еженедельного расписания");
        }

        String daysPart = parts[0];
        String time = parts[1];

        // Конвертируем дни недели в cron-формат (1=ВС, 2=ПН, ..., 7=СБ в cron)
        String cronDays = convertDaysToCron(daysPart);
        String[] timeParts = time.split(":");

        String cron = String.format("0 %s %s ? * %s", timeParts[1], timeParts[0], cronDays);
        String description = String.format("Еженедельно по %s в %s", formatDaysForDescription(daysPart), time);

        return new CronParseResult(cron, description, true);
    }

    private CronParseResult parseMonthlyFormat(String userInput) {
        String cleaned = userInput.replace("ежемесячно", "").trim();
        String[] parts = cleaned.split("\\s+");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Неверный формат ежемесячного расписания");
        }

        String daysPart = parts[0].replaceAll("\\s+", ",");
        String time = parts[1];
        String[] timeParts = time.split(":");

        String cron = String.format("0 %s %s %s * ?", timeParts[1], timeParts[0], daysPart);
        String description = String.format("Ежемесячно %s числа в %s", daysPart, time);

        return new CronParseResult(cron, description, true);
    }

    /**
     * Конвертирует дни недели в cron-формат
     */
    private String convertDaysToCron(String daysPart) {
        // Заменяем русские названия на цифры (1=ПН, 7=ВС)
        String normalized = daysPart
                .replace("пн", "2").replace("вт", "3").replace("ср", "4")
                .replace("чт", "5").replace("пт", "6").replace("сб", "7").replace("вс", "1")
                .toUpperCase();

        // Разбиваем на отдельные дни
        String[] days = normalized.split("[,\\s]+");
        StringBuilder result = new StringBuilder();

        for (String day : days) {
            if (!day.isEmpty()) {
                if (result.length() > 0) result.append(",");
                result.append(day);
            }
        }

        return result.toString();
    }

    private String formatDaysForDescription(String daysPart) {
        // Красивое форматирование для описания
        return daysPart.replace("пн", "понедельникам")
                .replace("вт", "вторникам")
                .replace("ср", "средам")
                .replace("чт", "четвергам")
                .replace("пт", "пятницам")
                .replace("сб", "субботам")
                .replace("вс", "воскресеньям");
    }

    /**
     * Генерирует человекочитаемое описание для cron-выражения
     */
    private String generateDescription(String cronExpression) {
        // Простая реализация - можно расширить при необходимости
        return "Расписание: " + cronExpression;
    }

    /**
     * Проверяет, должно ли выполняться расписание в указанное время
     * TODO хз, наверное можно сравнить как то проще, без плясок с бубнами, но пока как то так.
     */
    public boolean shouldExecute(String cronExpression, LocalDateTime dateTime) {
        try {
            CronExpression parsedExpression = CronExpression.parse(cronExpression);
            // CronExpression в спринговой библиотеке не имеет метода isSatisfiedBy как в либе Quartz
            // поэтому мы вычисляем следующее время выполнения, которое было бы до текущего момента. Для этого берём
            // текущее время, отнимаем от него 1 наносекунду и находим следующее время выполнения cron с помощью next()
            LocalDateTime checkPoint = dateTime.minusNanos(1);
            LocalDateTime nextExecutionTime = parsedExpression.next(checkPoint);
            return dateTime.equals(nextExecutionTime);
        } catch (Exception e) {
            log.error("Error checking cron expression: {}", cronExpression, e);
            return false;
        }
    }
}