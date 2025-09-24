package ru.kodrul.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot")
@PropertySource("classpath:application.yaml")
public class BotProperties {

    String name;
    String token;
    Integer botCreator;
}
