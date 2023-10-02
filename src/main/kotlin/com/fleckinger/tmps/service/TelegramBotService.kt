package com.fleckinger.tmps.service

import com.fleckinger.tmps.config.PropertiesConfig
import com.fleckinger.tmps.model.User
import com.fleckinger.tmps.utils.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.DateTimeException


@Service
class TelegramBotService(
    private val properties: PropertiesConfig.TelegramProperties,
    private val userService: UserService
) : TelegramLongPollingBot(properties.botToken) {

    private val className = this.javaClass.name

    private val startMessage = "Start message"

    private val helpMessage = "Help message"

    private val log: Logger = LoggerFactory.getLogger(TelegramBotService::class.java)

    override fun getBotUsername(): String {
        return properties.botName
    }

    override fun onUpdateReceived(update: Update?) {
        try {
            processTelegramUpdateEvent(update!!)
        } catch (e: Exception) {
            log.error(e.stackTrace.toString())
            if (update!!.hasMessage()) sendMessage(update.message.from.id, "Unknown error")
        }

    }

    private fun processTelegramUpdateEvent(update: Update) {
        if (hasCommandMessage(update)) {
            processCommand(
                update.message.chatId,
                update.message.text,
                update.message.from.id,
                update.message.from.userName
            )
        }
    }

    private fun processCommand(chatId: Long, command: String, telegramUserId: Long, username: String) {
        log.info("Class: $className. Method: processCommand(chatId: Long, command: String, telegramUserId: Long). Arguments:  chatId=$chatId, command=$command, telegramUserId=$telegramUserId")
        val words = command.split("\\s+".toRegex())
        when (words[0]) {
            "/start" -> startCommand(chatId, username, telegramUserId)

            "/set_channelId" -> setChannelIdCommand(chatId, words, telegramUserId)

            "/set_timezone" -> setTimezoneCommand(chatId, words, telegramUserId)

            "/help" -> helpCommand(chatId)

            else -> sendMessage(chatId, "'${words[0]}' is unknown command. Use /help to get commands")
        }
    }

    private fun registerUser(telegramUserId: Long, username: String): User {
        return if (userService.exists(telegramUserId)) {
            userService.getUserByTelegramUserId(telegramUserId)
        } else {
            userService.newUser(telegramUserId, username)
        }
    }

    private fun startCommand(chatId: Long, username: String, telegramUserId: Long) {
        registerUser(telegramUserId, username)
        sendMessage(chatId, startMessage)
    }

    private fun setChannelIdCommand(chatId: Long, words: List<String>, telegramUserId: Long) {
        try {
            when (words.size) {
                0 -> sendMessage(chatId, "Command shouldn't be empty")

                1 -> sendMessage(chatId, "Enter channel id.")

                2 -> {
                    userService.setChannelId(words[1].toLong(), telegramUserId)
                    sendMessage(chatId, "Channel id set.")
                }
            }
        } catch (nfe: java.lang.NumberFormatException) {
            log.error("Incorrect channel id ${words[1]} ")
            sendMessage(
                chatId,
                "Channel id '${words[1]}' is incorrect. You can get channel id from this bot @username\\_to\\_id\\_bot"
            )
        } catch (e: Exception) {
            log.error("Message: ${e.message}. \n Stack trace ${e.stackTraceToString()}")
            sendMessage(chatId, "Unknown error.")
        }

    }

    private fun setTimezoneCommand(chatId: Long, words: List<String>, telegramUserId: Long) {
        when (words.size) {
            0 -> sendMessage(chatId, "Command shouldn't be empty")

            1 -> sendMessage(chatId, "Enter timezone offset.")

            2 -> {
                try {
                    val zoneId = Utils.getTimezoneId(words[1].toInt())
                    userService.setTimezone(telegramUserId, zoneId)
                    sendMessage(chatId, "Timezone set.")
                } catch (nfe: java.lang.NumberFormatException) {
                    log.error("Incorrect timezone ${words[1]} ")
                    sendMessage(chatId, "Timezone '${words[1]}' is incorrect. Enter timezone like '+0' for UTC+0")
                } catch (dte: DateTimeException) {
                    log.error("Incorrect timezone ${words[1]} ")
                    sendMessage(chatId, "${dte.message} Enter timezone like '+0' for UTC+0")
                } catch (e: Exception) {
                    log.error("Message: ${e.message}. \n Stack trace ${e.stackTraceToString()}")
                    sendMessage(chatId, "Unknown error.")
                }
            }
        }

    }

    private fun helpCommand(chatId: Long) {
        sendMessage(chatId, helpMessage)
    }

    private fun sendMessage(chatId: Long, message: String) {
        val responseMessage = SendMessage(chatId.toString(), message)
        responseMessage.enableMarkdown(true)
        execute(responseMessage)
    }

    private fun sendPhoto(chatId: Long, photoId: String, text: String) {
        val responseMessage = SendPhoto()
        responseMessage.setChatId(chatId)
        responseMessage.photo = InputFile().setMedia(photoId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    private fun sendMediaGroup(chatId: Long, mediaIdList: List<String>) {
        //the trick is to set caption property only for the first element of an array. In this case telegram will show that caption below the media content
        val responseMessage = SendMediaGroup()
    }

    private fun hasCommandMessage(update: Update): Boolean {
        return update.hasMessage() && update.message.hasText() && update.message.text.startsWith("/")
    }
}