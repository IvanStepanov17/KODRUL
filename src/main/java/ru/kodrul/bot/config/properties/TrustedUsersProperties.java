package ru.kodrul.bot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "bot.trusted")
public class TrustedUsersProperties {

    private List<Long> userIds;
    private String adminKey;
}