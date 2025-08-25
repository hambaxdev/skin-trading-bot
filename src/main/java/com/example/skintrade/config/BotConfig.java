package com.example.skintrade.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class BotConfig {

    @Value("${bot.token}")
    private String token;

    @Value("${bot.username}")
    private String username;
}