package ru.kodrul.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot.rate-limiting")
public class RateLimitProperties {
    // TODO нужно ли и где возможно использовать?
    private int messagesPerSecond;
    private int maxMessagesPerMinute;
    private boolean enabled;
}