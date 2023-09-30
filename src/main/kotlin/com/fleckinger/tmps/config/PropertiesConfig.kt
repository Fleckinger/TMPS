package com.fleckinger.tmps.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan
class PropertiesConfig {

    @ConfigurationProperties(prefix = "telegram")
    data class TelegramProperties(
        val botToken: String,
        val botName: String,
        val channelId: String
    )

}