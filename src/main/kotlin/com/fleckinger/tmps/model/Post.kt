package com.fleckinger.tmps.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "post")
class Post(
    @Id
    @GeneratedValue
    @Column(name = "id")
    var id: UUID? = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_user_post"))
    var user: User? = null,

    @Column(name = "media_group_id")
    var mediaGroupId: String? = null,

    @Column(name = "text")
    var text: String? = null,

    @OneToMany(mappedBy = "post", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    var media: MutableList<Media>? = mutableListOf(),

    @Column(name = "post_date")
    var postDate: LocalDateTime? = null,

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
        if (mediaGroupId != other.mediaGroupId) return false
        if (text != other.text) return false
        if (media != other.media) return false
        if (postDate != other.postDate) return false
        if (isPosted != other.isPosted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (user?.hashCode() ?: 0)
        result = 31 * result + (mediaGroupId?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (media?.hashCode() ?: 0)
        result = 31 * result + (postDate?.hashCode() ?: 0)
        result = 31 * result + (isPosted?.hashCode() ?: 0)
        return result
    }



    fun hasMediaGroupId(): Boolean {
        return !mediaGroupId.isNullOrEmpty()
    }

    fun hasText(): Boolean {
        return !text.isNullOrEmpty()
    }

    fun hasMedia(): Boolean {
        return !media.isNullOrEmpty()
    }

    fun hasPostDate(): Boolean {
        return postDate != null
    }

    override fun toString(): String {
        return "Post(id=$id, user=$user, mediaGroupId=$mediaGroupId, text=$text, media=$media, postDate=$postDate, isPosted=$isPosted)"
    }
}

