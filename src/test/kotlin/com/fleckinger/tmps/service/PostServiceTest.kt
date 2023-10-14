package com.fleckinger.tmps.service

import com.fleckinger.tmps.model.Media
import com.fleckinger.tmps.model.Post
import com.fleckinger.tmps.repository.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.*

class PostServiceTest {

    @Mock
    lateinit var postRepositoryMock: PostRepository

    @InjectMocks
    lateinit var postService: PostService

    init {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun save_shouldSaveAndReturnSavedPost() {
        val post = Post()

        `when`(postRepositoryMock.save(any())).thenAnswer{ a -> a.arguments[0] }

        assertEquals(post, postRepositoryMock.save(post))
    }

    @Test
    fun delete_shouldCallDeleteMethodInRepository() {
        val post = Post()

        doNothing().`when`(postRepositoryMock).delete(any())

        postService.delete(post)

        verify(postRepositoryMock).delete(post)
    }

    @Test
    fun getPostByMediaGroupId_shouldReturnOptionalOfPostWithPassedMediaGroupId() {
        val mediaGroupId = "mediaGroupId"
        val post = Post(mediaGroupId = mediaGroupId)

        `when`(postRepositoryMock.findByMediaGroupId(mediaGroupId)).thenReturn(Optional.of(post))

        assertEquals(Optional.of(post), postService.getPostByMediaGroupId(mediaGroupId))
    }

    @Test
    fun getPostByTelegramMessageId_shouldReturnOptionalOfPostWithPasseTelegramMessageId() {
        val telegramMessageId = 100
        val post = Post(telegramMessageId = telegramMessageId)

        `when`(postRepositoryMock.findByTelegramMessageId(telegramMessageId)).thenReturn(Optional.of(post))

        assertEquals(Optional.of(post), postService.getPostByTelegramMessageId(telegramMessageId))
    }

    @Test
    fun countRemainingPost_shouldReturnLong() {
        val telegramUserId = 100L

        `when`(postRepositoryMock.countAllByIsPostedAndUser_TelegramUserId(false, telegramUserId)).thenReturn(1)

        assertEquals(1, postService.countRemainingPost(telegramUserId))
    }

    @Test
    fun postExistsByMediaGroupId_shouldReturnBoolean() {
        val mediaGroupId = "mediaGroupId"

        `when`(postRepositoryMock.existsByMediaGroupId(mediaGroupId)).thenReturn(true)

        assertEquals(true, postService.postExistsByMediaGroupId(mediaGroupId))
    }

    @Test
    fun getPostsBetweenDates_shouldReturnPost() {
        val startDate = LocalDateTime.now().minusMinutes(1)
        val endDate = LocalDateTime.now().plusMinutes(1)
        val posts = listOf(Post(), Post())

        `when`(postRepositoryMock.findAllByPostDateBetweenAndIsPosted(startDate, endDate, true)).thenReturn(posts)

        assertEquals(posts, postService.getPostsBetweenDates(startDate, endDate, true))
    }

    @Test
    fun hasOnlyText_shouldReturnTrueIfPostHasText_andDoesntHaveMedia() {
        val post = Post(text = "text")

        assertEquals(true, postService.hasOnlyText(post))
    }

    @Test
    fun hasOnlyText_shouldReturnTrueIfPostHasMedia_andDoesntHaveMediaGroup() {
        val post = Post(media = mutableListOf(Media()))

        assertEquals(true, postService.hasSingleMedia(post))
    }

    @Test
    fun hasOnlyText_shouldReturnTrueIfPostHasMediaGroup() {
        val post = Post(mediaGroupId = "mediaGroupId")

        assertEquals(true, postService.hasMediaGroup(post))
    }
}