package com.fleckinger.tmps.repository

import com.fleckinger.tmps.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository: JpaRepository<User, UUID> {
    fun findUserByTelegramUserId(telegramUserId: Long) : Optional<User>

    fun existsByTelegramUserId(telegramUserId: Long) : Boolean
}