package com.fleckinger.tmps.repository

import com.fleckinger.tmps.model.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface PostRepository: JpaRepository<Post, UUID> {
    fun findByMediaGroupId(mediaGroupId: String): Optional<Post>

    fun findByTelegramMessageId(telegramMessageId: Int): Optional<Post>

    fun existsByMediaGroupId(mediaGroupId: String): Boolean

    fun findAllByPostDateBetweenAndIsPosted(startDate: LocalDateTime, endDate: LocalDateTime, isPosted: Boolean): List<Post>

    fun countAllByIsPostedAndUser_TelegramUserId(isPosted: Boolean, telegramUserId: Long): Long

    fun deleteAllByIsPosted(isPosted: Boolean)
}