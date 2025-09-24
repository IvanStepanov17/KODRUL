package ru.kodrul.bot.utils;

public class Helper {

    /**
     * Экранирование специальных символов для MarkdownV2
     */
    public static String escapeMarkdownV2(String text) {
        if (text == null) return "";

        // Экранируем специальные символы MarkdownV2
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}
