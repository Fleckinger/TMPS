package com.fleckinger.tmps.repository

import com.fleckinger.tmps.model.Media
import org.springframework.data.jpa.repository.JpaRepository

interface MediaRepository: JpaRepository<Media, Long> {
}