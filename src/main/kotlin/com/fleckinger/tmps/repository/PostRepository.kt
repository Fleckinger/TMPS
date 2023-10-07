package com.fleckinger.tmps.repository

import com.fleckinger.tmps.model.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PostRepository: JpaRepository<Post, UUID> {
    fun findByMediaGroupId(mediaGroupId: String): Optional<Post>

    fun existsByMediaGroupId(mediaGroupId: String): Boolean
}