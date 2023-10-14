package com.fleckinger.tmps.service

import com.fleckinger.tmps.model.Post
import com.fleckinger.tmps.repository.PostRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class PostService(private val postRepository: PostRepository) {

    val log: Logger = LoggerFactory.getLogger(PostRepository::class.java)

    fun save(post: Post): Post {
        log.info("Saving post $post")
        return postRepository.save(post)
    }

    fun delete(post: Post) {
        postRepository.delete(post)
    }

    fun getPostByMediaGroupId(mediaGroupId: String): Optional<Post> {
        return postRepository.findByMediaGroupId(mediaGroupId)
    }

    fun getPostByTelegramMessageId(telegramMessageId: Int): Optional<Post> {
        return postRepository.findByTelegramMessageId(telegramMessageId)
    }

    fun countRemainingPost(telegramUserId: Long): Long {
        return postRepository.countAllByIsPostedAndUser_TelegramUserId(false, telegramUserId)
    }

    fun postExistsByMediaGroupId(mediaGroupId: String): Boolean {
        return postRepository.existsByMediaGroupId(mediaGroupId)
    }

    fun getPostsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime, isPosted: Boolean): List<Post> {
        return postRepository.findAllByPostDateBetweenAndIsPosted(startDate, endDate, isPosted)
    }

    fun hasOnlyText(post: Post): Boolean {
        return post.hasText() && (!post.hasMediaGroupId() && !post.hasMedia())
    }
    fun hasSingleMedia(post: Post): Boolean {
        return post.hasMedia() && !post.hasMediaGroupId()
    }

    fun hasMediaGroup(post: Post): Boolean {
        return post.hasMediaGroupId()
    }
}