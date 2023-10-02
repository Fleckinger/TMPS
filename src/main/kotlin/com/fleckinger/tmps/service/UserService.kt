package com.fleckinger.tmps.service

import com.fleckinger.tmps.model.User
import com.fleckinger.tmps.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import java.time.ZoneId

import java.util.UUID

@Service
class UserService(private val userRepository: UserRepository) {

    fun getUser(id: String): User {
        return userRepository.findById(UUID.fromString(id))
            .orElseThrow { EntityNotFoundException("User with [ID = $id] not found") }
    }

    fun exists(telegramUserId: Long): Boolean {
        return userRepository.findUserByTelegramUserId(telegramUserId).isPresent
    }

    fun newUser(telegramUserId: Long, username: String): User {
        val user = User(telegramUserId = telegramUserId, username = username)
        return saveUser(user)
    }

    fun getUserByTelegramUserId(telegramUserId: Long): User {
        return userRepository.findUserByTelegramUserId(telegramUserId)
            .orElseThrow { EntityNotFoundException("User with [TELEGRAM_USER_ID = $telegramUserId] not found") }
    }

    fun saveUser(user: User): User {
        return userRepository.save(user)
    }

    fun setChannelId(chatId: Long, telegramUserId: Long): Long {
        var user = getUserByTelegramUserId(telegramUserId)
        user.channelId = chatId
        user = saveUser(user)
        return user.channelId!!
    }

    fun setTimezone(telegramUserId: Long, zoneId: ZoneId): String {
        var user = getUserByTelegramUserId(telegramUserId)
        user.timeZone = zoneId.id
        user = saveUser(user)
        return user.timeZone!!
    }
}
