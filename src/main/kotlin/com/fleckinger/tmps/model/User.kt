package com.fleckinger.tmps.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "usr")
class User(
    @Id
    @GeneratedValue
    @Column(name = "id")
    var id: UUID? = null,

    @Column(name = "telegram_user_id")
    var telegramUserId: Long? = null,

    @Column(name = "channel_id")
    var channelId: Long? = null,

    @Column(name = "username")
    var username: String? = null,

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var posts: List<Post> = emptyList(),

    @Column(name = "time_zone")
    var timeZone: String? = null

) {
    val idString
        get() = id?.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (telegramUserId != other.telegramUserId) return false
        if (channelId != other.channelId) return false
        if (username != other.username) return false
        if (posts != other.posts) return false
        if (timeZone != other.timeZone) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (telegramUserId?.hashCode() ?: 0)
        result = 31 * result + (channelId?.hashCode() ?: 0)
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + posts.hashCode()
        result = 31 * result + (timeZone?.hashCode() ?: 0)
        return result
    }


}