package com.fleckinger.tmps.service

import com.fleckinger.tmps.model.Media
import com.fleckinger.tmps.repository.MediaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class MediaServiceTest {

    @Mock
    lateinit var mediaRepositoryMock: MediaRepository

    @InjectMocks
    lateinit var mediaService: MediaService

    init {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun save_shouldSaveAndReturnSavedMedia() {
        val media = Media()

        `when`(mediaRepositoryMock.save(any())).thenAnswer{ a -> a.arguments[0] }

        assertEquals(media, mediaService.save(media))
    }
}