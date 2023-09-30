package com.fleckinger.tmps.model

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "post")
class Post(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_post_user"))
    var user: User? = null,

    @Column(name = "text")
    var text: String? = null,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var media: List<Media>? = emptyList(),

    @Column(name = "post_date")
    var postDate: ZonedDateTime? = null,

    @Column(name = "is_posted")
    var isPosted: Boolean? = false

) {
    val idString
        get() = id?.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Post

        if (id != other.id) return false
        if (user != other.user) return false
        if (text != other.text) return false
        if (media != other.media) return false
        if (postDate != other.postDate) return false
        if (isPosted != other.isPosted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (user?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (media?.hashCode() ?: 0)
        result = 31 * result + (postDate?.hashCode() ?: 0)
        result = 31 * result + (isPosted?.hashCode() ?: 0)
        return result
    }


}

