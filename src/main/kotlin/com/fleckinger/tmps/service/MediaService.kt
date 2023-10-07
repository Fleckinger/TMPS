package com.fleckinger.tmps.service

import com.fleckinger.tmps.model.Media
import com.fleckinger.tmps.repository.MediaRepository
import org.springframework.stereotype.Service

@Service
class MediaService(private val mediaRepository: MediaRepository) {
    fun save(media: Media): Media {
        return mediaRepository.save(media)
    }
}