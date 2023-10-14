package com.fleckinger.tmps.service

import com.fleckinger.tmps.config.PropertiesConfig
import com.fleckinger.tmps.dto.MediaTypes
import com.fleckinger.tmps.exception.BotNotAddedToChannelException
import com.fleckinger.tmps.exception.IllegalPostDateException
import com.fleckinger.tmps.exception.RegistrationNotCompletedException
import com.fleckinger.tmps.model.Media
import com.fleckinger.tmps.model.Post
import com.fleckinger.tmps.model.User
import com.fleckinger.tmps.utils.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.media.InputMedia
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAnimation
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TelegramBotService(
    private val telegramProperties: PropertiesConfig.TelegramProperties,
    private val userService: UserService,
    private val postService: PostService,
) : TelegramLongPollingBot(telegramProperties.botToken) {

    private val className = this.javaClass.name
    private val dateTimePattern = "(\\d{2})-(\\d{2})-(\\d{4})\\s(\\d{2}):(\\d{2})".toRegex()

    private val log: Logger = LoggerFactory.getLogger(TelegramBotService::class.java)

    override fun getBotUsername(): String {
        return telegramProperties.botName
    }

    override fun onUpdateReceived(update: Update?) {
        try {
            processTelegramUpdateEvent(update!!)
        } catch (e: Exception) {
            log.error(e.stackTraceToString())
            if (update!!.hasMessage()) sendTextMessage(update.message.from.id, "Unknown error")
        }
    }

    @Scheduled(fixedDelayString = "\${application.posting-delay}")
    private fun sendScheduledPost() {
        val startDate = LocalDateTime.now().minusMinutes(1)
        val endDate = LocalDateTime.now()

        val posts = postService.getPostsBetweenDates(startDate, endDate, false)

        posts.forEach {
            val chatId = it.user!!.channelId!!

            if (postService.hasOnlyText(it)) {
                sendTextMessage(chatId, it.text!!)
            } else if (postService.hasSingleMedia(it)) {
                sendSingleMediaMessage(chatId, it.media!![0].fileId!!, MediaTypes.valueOf(it.media!![0].type), it.text)
            } else if (postService.hasMediaGroup(it)) {
                sendMediaGroupMessage(chatId, it.media!!, it.text)
            }
            it.isPosted = true
            postService.save(it)
        }
    }

    fun sendTextMessage(chatId: Long, text: String) {
        val responseMessage = SendMessage(chatId.toString(), text)
        execute(responseMessage)
    }

    fun replyToMessageWithText(chatId: Long, replyToMessageId: Int, text: String) {
        val responseMessage = SendMessage.builder().chatId(chatId).replyToMessageId(replyToMessageId).text(text).build()
        execute(responseMessage)
    }

    fun sendSingleMediaMessage(chatId: Long, mediaId: String, mediaType: MediaTypes, text: String?) {
        when (mediaType) {
            MediaTypes.IMAGE -> sendPhotoMessage(chatId, mediaId, text ?: "")
            MediaTypes.VIDEO -> sendVideoMessage(chatId, mediaId, text ?: "")
            MediaTypes.AUDIO -> sendAudioMessage(chatId, mediaId, text ?: "")
            MediaTypes.DOCUMENT -> sendDocumentMessage(chatId, mediaId, text ?: "")
            MediaTypes.ANIMATION -> sendAnimationMessage(chatId, mediaId, text ?: "")
            MediaTypes.NONE -> "doNothing"
        }
    }

    fun sendMediaGroupMessage(chatId: Long, medias: MutableList<Media>, text: String?) {
        if (medias.size < 2 || medias.size > 10) throw IllegalArgumentException("Media list must include 2-10 items.")

        val telegramMedias = medias.stream().map {
            when (MediaTypes.valueOf(it.type)) {
                MediaTypes.IMAGE -> InputMediaPhoto.builder().media(it.fileId!!).build()
                MediaTypes.VIDEO -> InputMediaVideo.builder().media(it.fileId!!).build()
                MediaTypes.AUDIO -> InputMediaAudio.builder().media(it.fileId!!).build()
                MediaTypes.ANIMATION -> InputMediaAnimation.builder().media(it.fileId!!).build()
                MediaTypes.DOCUMENT -> InputMediaDocument.builder().media(it.fileId!!).build()
                MediaTypes.NONE -> InputMediaPhoto.builder().build()
            }
        }.toList()
        //the trick is to set caption property only for the first element of an array. In this case telegram will show that caption below the media content
        telegramMedias[0].caption = text ?: ""

        val responseMessage = SendMediaGroup.builder()
            .chatId(chatId)
            .medias(telegramMedias as List<InputMedia>)
            .build()
        execute(responseMessage)
    }

    fun processTelegramUpdateEvent(update: Update) {
        val message = update.message ?: update.editedMessage
        when {
            isCommandMessage(update) -> processCommand(message)
            else -> processMessage(message, update.hasEditedMessage())
        }
    }

    fun processMessage(message: Message, isUpdate: Boolean) {
        val user = userService.getUserByTelegramUserId(message.from.id)

        try {
            userService.checkUserRegistration(user)
            if (isUpdate) {
                updatePost(user, message)
            } else {
                createPost(user, message)
            }

        } catch (e: DateTimeException) {
            sendTextMessage(message.chatId, "Wrong date format")
        } catch (e: RegistrationNotCompletedException) {
            log.error("User ${user.username} registration is not completed. Error message: ${e.message}")
            sendTextMessage(message.chatId, e.message!!)
        } catch (e: BotNotAddedToChannelException) {
            log.error("User ${user.username} not added the bot to the channel ${user.channelId} as an administrator. Error message: ${e.message}")
            sendTextMessage(message.chatId, e.message!!)
        } catch (e: IllegalPostDateException) {
            log.error("User ${user.username} send post to channel ${user.channelId} with past date. Error message: ${e.message}")
            sendTextMessage(message.chatId, e.message!!)
        }
    }

    fun updatePost(user: User, message: Message) {
        if (!isBotAddedToUserChannel(user.channelId!!)) throw BotNotAddedToChannelException("Bot not added to the channel as an administrator.")
        val text = getText(message)
        val postOptional = postService.getPostByTelegramMessageId(message.messageId)
            .filter { post -> post.user?.telegramUserId == user.telegramUserId }
        if (postOptional.isPresent) {
            val timestamp = user.timeZone?.let { getTimestampWithAppTimezone(text, it) }
            if (timestamp!!.isBefore(LocalDateTime.now())) throw IllegalPostDateException("The date cannot be past.")
            val post = postOptional.get()
            post.text = removeTimestamp(text)
            post.postDate = timestamp
            postService.save(post)
            replyToMessageWithText(message.chatId, message.messageId, "Post edited.")
        } else {
            replyToMessageWithText(message.chatId, message.messageId, "Post is not found in scheduled.")
        }
    }

    fun createPost(user: User, message: Message) {
        if (!isBotAddedToUserChannel(user.channelId!!)) throw BotNotAddedToChannelException("Bot not added to the channel as an administrator.")
        val text = getText(message)
        val post: Post
        if (message.mediaGroupId == null || isFirstMessageInGroup(message)) {
            //process single attachment/first message in group
            val media = if (hasMedia(message)) mutableListOf(getMedia(message)) else mutableListOf()
            val timestamp = user.timeZone?.let { getTimestampWithAppTimezone(text, it) }
            if (timestamp!!.isBefore(LocalDateTime.now())) throw IllegalPostDateException("The date cannot be past.")
            post = Post(
                user = user,
                text = removeTimestamp(text),
                media = media,
                telegramMessageId = message.messageId,
                mediaGroupId = message.mediaGroupId,
                postDate = timestamp
            )
            replyToMessageWithText(message.chatId, message.messageId, "Post successfully scheduled.")
        } else {
            //process multiple attachments
            post = postService.getPostByMediaGroupId(message.mediaGroupId).get()
            post.media!!.add(getMedia(message))
        }

        post.media!!.forEach { m -> m.post = post }
        postService.save(post)
    }

    fun registerUser(telegramUserId: Long, username: String): User {
        return if (userService.exists(telegramUserId)) {
            userService.getUserByTelegramUserId(telegramUserId)
        } else {
            userService.createNewUser(telegramUserId, username)
        }
    }

    /**
     * Method parses the date, and converts it to app timezone
     */
    fun getTimestampWithAppTimezone(text: String, timezoneId: String): LocalDateTime {
        //timestamp patter dd-MM-yyyy HH:mm

        val timezone = ZoneId.of(timezoneId)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm z").withZone(timezone)

        if (text.contains(dateTimePattern)) {
            val dateString = "${dateTimePattern.find(text)!!.value} $timezone"
            return ZonedDateTime.parse(dateString, formatter).withZoneSameInstant(ZonedDateTime.now().zone)
                .toLocalDateTime()
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

    private fun processCommand(message: Message) {
        log.info("Class: $className. Method: processCommand(chatId: Long, command: String, telegramUserId: Long). Arguments:  chatId=${message.chatId}, command=${message.text}, telegramUserId=${message.from.id}")
        val words = message.text.split("\\s+".toRegex())
        val telegramUserId = message.from.id
        val chatId = message.chatId
        val username = message.from.userName
        val repliedMessageId = message.replyToMessage?.let { it.messageId }
        when (words[0]) {
            "/start" -> startCommand(chatId, username, telegramUserId)

            "/set_channelId" -> setChannelIdCommand(chatId, words, telegramUserId)

            "/set_timezone" -> setTimezoneCommand(chatId, words, telegramUserId)

            "/delete" -> deleteCommand(chatId, telegramUserId, repliedMessageId)

            "/edit_text" -> editTextCommand(chatId, telegramUserId, repliedMessageId, message.text)

            "/edit_date" -> editDateCommand(chatId, telegramUserId, repliedMessageId, message.text)

            "/remaining_posts" -> remainingPostCommand(chatId, telegramUserId)

            "/help" -> helpCommand(chatId)

            else -> sendTextMessage(chatId, "'${words[0]}' is unknown command. Use /help to get commands")
        }
    }

    private fun startCommand(chatId: Long, username: String, telegramUserId: Long) {
        if (!userService.exists(telegramUserId)) {
            registerUser(telegramUserId, username)
        }
        val startMessage = """Hello, $username. 
            |
            |Welcome to TMPS bot.
            |
            |This bot allow you to schedule messages with text and/or media. 
            |Currently supports images, videos, audio, documents.
            |
            |To use the bot follow these steps:
            |1) Add this bot to your channel, in which you want to post, as an administrator with "manage messages" permission. 
            |
            |2) Set the channel id in which you added the bot. You can get the id from this bot @username_to_id_bot 
            |Use command /set_channelId *your_channel_id* without asterisks.
            |
            |3) Set your time zone offset. For example - offset for London is +3. If your area uses daylight saving time(summer time), you will need to update your time zone when seasonal clocks change.
            |Use command /set_timezone *your_timezone* without asterisks.
            |
            |4) Send message with text and/or media and posting time in the following format dd-MM-yyyy HH:mm, for example 14-09-2024 13:02.
            |
            |To see all available commands, use /help command.
        """.trimMargin()
        sendTextMessage(chatId, startMessage)
    }

    private fun setChannelIdCommand(chatId: Long, words: List<String>, telegramUserId: Long) {
        try {
            when (words.size) {
                0 -> sendTextMessage(chatId, "Command shouldn't be empty")

                1 -> sendTextMessage(chatId, "Enter channel id.")

                2 -> {
                    userService.setChannelId(words[1].toLong(), telegramUserId)
                    sendTextMessage(chatId, "Channel id set.")
                }
            }
        } catch (nfe: java.lang.NumberFormatException) {
            log.error("Incorrect channel id ${words[1]} ")
            sendTextMessage(
                chatId,
                "Channel id '${words[1]}' is incorrect. You can get channel id from this bot @username\\_to\\_id\\_bot"
            )
        } catch (e: Exception) {
            log.error("Message: ${e.message}. \n Stack trace ${e.stackTraceToString()}")
            sendTextMessage(chatId, "Unknown error.")
        }

    }

    private fun setTimezoneCommand(chatId: Long, words: List<String>, telegramUserId: Long) {
        when (words.size) {
            0 -> sendTextMessage(chatId, "Command shouldn't be empty")

            1 -> sendTextMessage(chatId, "Enter timezone offset.")

            2 -> {
                try {
                    val zoneId = Utils.getTimezoneId(words[1].toInt())
                    userService.setTimezone(telegramUserId, zoneId)
                    sendTextMessage(chatId, "Timezone set.")
                } catch (nfe: java.lang.NumberFormatException) {
                    log.error("Incorrect timezone ${words[1]} ")
                    sendTextMessage(chatId, "Timezone '${words[1]}' is incorrect. Enter timezone like '+0' for UTC+0")
                } catch (dte: DateTimeException) {
                    log.error("Incorrect timezone ${words[1]} ")
                    sendTextMessage(chatId, "${dte.message} Enter timezone like '+0' for UTC+0")
                } catch (e: Exception) {
                    log.error("Message: ${e.message}. \n Stack trace ${e.stackTraceToString()}")
                    sendTextMessage(chatId, "Unknown error.")
                }
            }
        }
    }

    private fun deleteCommand(chatId: Long, telegramUserId: Long, messageId: Int?) {
        if (messageId != null) {
            val post = postService.getPostByTelegramMessageId(messageId)
                .filter { post -> post.user?.telegramUserId == telegramUserId }

            post.ifPresentOrElse({
                postService.delete(post.get())
                replyToMessageWithText(chatId, messageId, "Post deleted.")
            }, { replyToMessageWithText(chatId, messageId, "Post is not found in scheduled.") })

        } else {
            sendTextMessage(chatId, "To delete a post, reply to the original message.")
        }
    }

    private fun editTextCommand(chatId: Long, telegramUserId: Long, messageId: Int?, text: String) {
        val newText = text.replace("/edit_text ", "")
        if (messageId != null) {
            val post = postService.getPostByTelegramMessageId(messageId)
                .filter { post -> post.user?.telegramUserId == telegramUserId }

            post.ifPresentOrElse({
                it.text = newText
                postService.save(it)
                //TODO should be replied to original message
                sendTextMessage(chatId, "Post edited.")
            }, { sendTextMessage(chatId, "Post not found.") })

        } else {
            sendTextMessage(chatId, "To edit a post, reply to the original message.")
        }
    }

    private fun editDateCommand(chatId: Long, telegramUserId: Long, messageId: Int?, postDate: String) {
        val user = userService.getUserByTelegramUserId(telegramUserId)
        val timestamp = user.timeZone?.let { getTimestampWithAppTimezone(postDate, it) }
        if (timestamp!!.isBefore(LocalDateTime.now())) throw IllegalPostDateException("The date cannot be past.")

        if (messageId != null) {
            val post = postService.getPostByTelegramMessageId(messageId)
                .filter { post -> post.user?.telegramUserId == telegramUserId }

            post.ifPresentOrElse({
                it.postDate = LocalDateTime.now() //EDIT
                postService.save(it)
                //TODO should be replied to original message
                sendTextMessage(chatId, "Post date edited.")
            }, { sendTextMessage(chatId, "Post not found.") })

        } else {
            sendTextMessage(chatId, "To edit a post date, reply to the original message.")
        }
    }

    private fun remainingPostCommand(chatId: Long, telegramUserId: Long) {
        val remainingCount = postService.countRemainingPost(telegramUserId)
        sendTextMessage(chatId, "$remainingCount posts left.")
    }

    private fun helpCommand(chatId: Long) {
        val helpMessage = """Available commands:
            |
            |/start - show start message.
            |
            |/set_channelId *channel_id* without asterisks - set/update channel id in which you added the bot. You can get the id from this bot @username_to_id_bot
            |
            |/set_timezone *your_timezone* without asterisks - set/update your time zone offset. For example - offset for London is +3. If your area uses daylight saving time(summer time), you will need to update your time zone when seasonal clocks change.
            |
            |/delete - delete post from schedule, reply with this command to message which you want to delete. Please note that message will not be deleted from bot message history, and you need to delete it manually.
            |
            |/edit_text *new_text* without asterisks - edit post text, reply with this command and new text to message which you want to edit/
            |
            |/edit_date *new_post_date* without asterisks - edit post date, reply with this command to message which you want to edit. Date format is dd-MM-yyyy HH:mm, for example 14-09-2024 13:02.
            |
            |/remaining_posts - shows number of remaining scheduled posts. 
            |
            |/help - show help message.
            |
            |How to:
            
            |1) Send scheduled post - send message with text and/or media and posting time, in the following format dd-MM-yyyy HH:mm, for example 14-09-2024 13:02.
            |
            |2) Edit scheduled post text and/or post date - edit original message, enter new text and/or new post date. Or reply to original message with command /edit_text and enter new text or /edit_date and enter new date.
            |
            |3) Delete post from schedule - reply to original post with /delete command. Please note that message will not be deleted from bot message history, and you need to delete it manually.
        """.trimMargin()
        sendTextMessage(chatId, helpMessage)
    }

    private fun isCommandMessage(update: Update): Boolean {
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

    private fun isFirstMessageInGroup(message: Message): Boolean {
        return !postService.postExistsByMediaGroupId(message.mediaGroupId)
    }

    private fun hasMedia(message: Message): Boolean {
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

    private fun removeTimestamp(text: String): String {
        return text.replace(dateTimePattern, "")
    }

    private fun sendPhotoMessage(chatId: Long, photoId: String, text: String) {
        val responseMessage = SendPhoto()
        responseMessage.setChatId(chatId)
        responseMessage.photo = InputFile().setMedia(photoId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    private fun sendVideoMessage(chatId: Long, videoId: String, text: String) {
        val responseMessage = SendVideo()
        responseMessage.setChatId(chatId)
        responseMessage.video = InputFile().setMedia(videoId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    private fun sendAudioMessage(chatId: Long, audioId: String, text: String) {
        val responseMessage = SendAudio()
        responseMessage.setChatId(chatId)
        responseMessage.audio = InputFile().setMedia(audioId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    private fun sendAnimationMessage(chatId: Long, animationId: String, text: String) {
        val responseMessage = SendAnimation()
        responseMessage.setChatId(chatId)
        responseMessage.animation = InputFile().setMedia(animationId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    private fun sendDocumentMessage(chatId: Long, documentId: String, text: String) {
        val responseMessage = SendDocument()
        responseMessage.setChatId(chatId)
        responseMessage.document = InputFile().setMedia(documentId)
        responseMessage.caption = text
        execute(responseMessage)
    }

    private fun isBotAddedToUserChannel(channelId: Long): Boolean {
        return try {
            val administrators = execute(GetChatAdministrators.builder().chatId(channelId).build())
            administrators.stream().anyMatch { member -> member.user.id == telegramProperties.botId }
        } catch (e: Exception) {
            false
        }
    }
}