package com.bluestone.scienceexplorer.database

import com.bluestone.scienceexplorer.database.YoutubeVideoRequestEntity_.videoLinks
import io.objectbox.BoxStore
import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

const val APPROVED = "APPROVED"
const val DENIED = "DENIED"
const val IN_PROGRESS = "IN_PROGRESS"
const val NEW_LINK = "NEW_LINK"
const val DOES_NOT_EXIST = "DOES_NOT_EXIST"
enum class LinkRequestStatus {
    APPROVED,
    DENIED,
    IN_PROGRESS,
    NEW_LINK,
    DOES_NOT_EXIST,
    INVALID
}
@Entity
data class YoutubeVideoRequestEntity(
    @Id var id: Long = 0,
    val clientID: String = ""
) {
    @Backlink
    var videoLinks: ToMany<VideoLinkDescriptorEntity> = ToMany(this, YoutubeVideoRequestEntity_.videoLinks)
    @JvmField
    @Transient
    @Suppress("PropertyName")
    var __boxStore: BoxStore? = null
}

@Entity
data class VideoLinkDescriptorEntity(
    @Id var id: Long = 0,
    val link: String="", var status: String=""
) {
    var videoLink: ToOne<YoutubeVideoRequestEntity> = ToOne(this, VideoLinkDescriptorEntity_.videoLink)
}