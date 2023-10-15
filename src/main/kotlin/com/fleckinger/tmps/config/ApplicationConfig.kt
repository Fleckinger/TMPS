package com.fleckinger.tmps.config

import com.fleckinger.tmps.service.TelegramBotService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


@Configuration
class ApplicationConfig {
    @Bean
    fun telegramBotsApi(telegramBotService: TelegramBotService): TelegramBotsApi? {
        return TelegramBotsApi(DefaultBotSession::class.java).apply { registerBot(telegramBotService) }
    }
}