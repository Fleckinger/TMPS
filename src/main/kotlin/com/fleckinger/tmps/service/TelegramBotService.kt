package com.fleckinger.tmps.service

import com.fleckinger.tmps.config.PropertiesConfig
import com.fleckinger.tmps.dto.MediaTypes
import com.fleckinger.tmps.exception.RegistrationNotCompletedException
import com.fleckinger.tmps.model.Media
import com.fleckinger.tmps.model.Post
import com.fleckinger.tmps.model.User
import com.fleckinger.tmps.utils.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TelegramBotService(
    private val properties: PropertiesConfig.TelegramProperties,
    private val userService: UserService,
    private val postService: PostService,
) : TelegramLongPollingBot(properties.botToken) {

    private val className = this.javaClass.name
    private val startMessage = "Start message"
    private val helpMessage = "Help message"
    private val dateTimePattern = "(\\d{2})-(\\d{2})-(\\d{4})\\s(\\d{2}):(\\d{2})".toRegex()

    private val log: Logger = LoggerFactory.getLogger(TelegramBotService::class.java)


    override fun getBotUsername(): String {
        return properties.botName
    }

    override fun onUpdateReceived(update: Update?) {
        try {
            processTelegramUpdateEvent(update!!)
        } catch (e: Exception) {
            log.error(e.stackTraceToString())
            if (update!!.hasMessage()) sendMessage(update.message.from.id, "Unknown error")
        }
    }

    fun sendMessage(chatId: Long, message: String) {
        val responseMessage = SendMessage(chatId.toString(), message)
        responseMessage.enableMarkdown(true)
        execute(responseMessage)
    }

    fun sendPhoto(chatId: Long, photoId: String, text: String) {
        val responseMessage = SendPhoto()
        responseMessage.setChatId(chatId)
        responseMessage.photo = InputFile().setMedia(photoId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    fun sendVideo(chatId: Long, videoId: String, text: String) {
        val responseMessage = SendVideo()
        responseMessage.setChatId(chatId)
        responseMessage.video = InputFile().setMedia(videoId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    fun sendMediaGroup(chatId: Long, mediaIdList: List<String>) {
        //the trick is to set caption property only for the first element of an array. In this case telegram will show that caption below the media content
        val responseMessage = SendMediaGroup()

    }

    fun processTelegramUpdateEvent(update: Update) {
        val message = update.message
        when {
            isCommandMessage(update) -> processCommand(
                message.chatId,
                message.text,
                message.from.id,
                message.from.userName
            )
            else -> processMessage(message)
        }
    }

    fun processMessage(message: Message) {
        val user = userService.getUserByTelegramUserId(message.from.id)

        try {
            userService.checkUserRegistration(user)
            createPost(user, message)
        } catch (e: DateTimeException) {
            sendMessage(message.chatId, "Wrong date format")
        } catch (e: RegistrationNotCompletedException) {
            log.error("User ${user.username} registration is not completed. Error message: ${e.message}")
            sendMessage(message.chatId, e.message!!)
        }
    }

    fun createPost(user: User, message: Message) {
        val text = getText(message)
        val post: Post
        if (message.mediaGroupId == null || isFirstMessageInGroup(message)) {
            //process single attachment/first message in group
            val media = if (hasMedia(message)) mutableListOf(getMedia(message)) else mutableListOf()
            val timestamp = user.timeZone?.let { getTimestampWithAppTimezone(text, it) }
            post = Post(
                user = user,
                text = removeTimestamp(text),
                media = media,
                mediaGroupId = message.mediaGroupId,
                postDate = timestamp
            )
            sendMessage(message.chatId, "Post successfully scheduled.")
        } else {
            //process multiple attachments
            post = postService.getPostByMediaGroupId(message.mediaGroupId).get()
            post.media!!.add(getMedia(message))
        }

        post.media!!.forEach { m -> m.post = post }
        postService.save(post)
    }

    private fun isFirstMessageInGroup(message: Message): Boolean {
        return !postService.postExistsByMediaGroupId(message.mediaGroupId)
    }

    fun registerUser(telegramUserId: Long, username: String): User {
        return if (userService.exists(telegramUserId)) {
            userService.getUserByTelegramUserId(telegramUserId)
        } else {
            userService.newUser(telegramUserId, username)
        }
    }

    fun isCommandMessage(update: Update): Boolean {
        return update.hasMessage()
                && !update.message.hasPhoto()
                && !update.message.hasVideo()
                && !update.message.hasAnimation()
                && !update.message.hasAudio()
                && !update.message.hasDocument()
                && !update.message.hasLocation()
                && update.message.hasText()
                && update.message.text.startsWith("/")

    }

    fun processCommand(chatId: Long, command: String, telegramUserId: Long, username: String) {
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

    fun startCommand(chatId: Long, username: String, telegramUserId: Long) {
        registerUser(telegramUserId, username)
        sendMessage(chatId, startMessage)
    }

    fun setChannelIdCommand(chatId: Long, words: List<String>, telegramUserId: Long) {
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

    fun setTimezoneCommand(chatId: Long, words: List<String>, telegramUserId: Long) {
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

    fun helpCommand(chatId: Long) {
        sendMessage(chatId, helpMessage)
    }

    /**
     * Method parses the date, and converts it to app timezone
     */
    fun getTimestampWithAppTimezone(text: String, timezoneId: String): ZonedDateTime {
        //timestamp patter dd-MM-yyyy HH:mm

        val timezone = ZoneId.of(timezoneId)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm z").withZone(timezone)

        if (text.contains(dateTimePattern)) {
            val dateString = "${dateTimePattern.find(text)!!.value} $timezone"
            return ZonedDateTime.parse(dateString, formatter).withZoneSameInstant(ZonedDateTime.now().zone)
        } else {
            throw DateTimeException("Wrong date format")
        }
    }

    fun getText(message: Message): String {
        val text = if (message.hasText()) message.text else message.caption
        return if (!text.isNullOrEmpty()) {
            text
        } else {
            ""
        }
    }

    fun removeTimestamp(text: String): String {
        return text.replace(dateTimePattern, "")
    }

    fun getMedia(message: Message): Media {
        return when {
            message.hasVideo() -> Media(type = MediaTypes.VIDEO.name, fileId = message.video.fileId)
            message.hasPhoto() -> Media(
                type = MediaTypes.IMAGE.name,
                fileId = message.photo[message.photo.size - 1].fileId
            )
            message.hasAudio() -> Media(type = MediaTypes.AUDIO.name, fileId = message.audio.fileId)
            message.hasVoice() -> Media(type = MediaTypes.AUDIO.name, fileId = message.voice.fileId)
            message.hasDocument() -> Media(type = MediaTypes.DOCUMENT.name, fileId = message.document.fileId)
            message.hasAnimation() -> Media(type = MediaTypes.ANIMATION.name, fileId = message.animation.fileId)
            else -> Media()
        }
    }

    fun hasMedia(message: Message): Boolean {
        return when {
            message.hasVideo() -> true
            message.hasPhoto() -> true
            message.hasAudio() -> true
            message.hasVoice() -> true
            message.hasDocument() -> true
            message.hasAnimation() -> true
            else -> false
        }
    }
}