package ru.kodrul.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class BotApplication {

	public static void main(String[] args) throws TelegramApiException {
		var app = SpringApplication.run(BotApplication.class, args);
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
		botsApi.registerBot(app.getBean("kodRulBot", AbilityBot.class));
	}

}
