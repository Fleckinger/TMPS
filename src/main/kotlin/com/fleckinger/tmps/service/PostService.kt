package com.fleckinger.tmps.service

import com.fleckinger.tmps.model.Post
import com.fleckinger.tmps.repository.PostRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PostService(private val postRepository: PostRepository) {

    val log: Logger = LoggerFactory.getLogger(PostRepository::class.java)

    fun save(post: Post): Post {
        log.info("Saving post $post")
        return postRepository.save(post)
    }

    fun getPostByMediaGroupId(mediaGroupId: String): Optional<Post> {
        return postRepository.findByMediaGroupId(mediaGroupId)
    }

    fun postExistsByMediaGroupId(mediaGroupId: String): Boolean {
        return postRepository.existsByMediaGroupId(mediaGroupId)
    }


}