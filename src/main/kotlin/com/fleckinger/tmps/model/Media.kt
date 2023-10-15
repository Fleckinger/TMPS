package com.fleckinger.tmps.model

import com.fleckinger.tmps.dto.MediaTypes
import jakarta.persistence.*

@Entity
@Table(name = "media")
class Media(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "type")
    val type: String = MediaTypes.NONE.name,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", foreignKey = ForeignKey(name = "fk_media_post"))
    var post: Post? = null,

    @Column(name = "file_id")
    val fileId: String? = null,

    @Column(name = "index")
    val index: Int? = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Media

        if (id != other.id) return false
        if (type != other.type) return false
        if (fileId != other.fileId) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        result = 31 * result + (fileId?.hashCode() ?: 0)
        result = 31 * result + (index ?: 0)
        return result
    }

    override fun toString(): String {
        return "Media(id=$id, type='$type', fileId=$fileId, index=$index)"
    }


}