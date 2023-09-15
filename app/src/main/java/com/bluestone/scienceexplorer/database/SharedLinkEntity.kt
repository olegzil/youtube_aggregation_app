package com.bluestone.scienceexplorer.database

import com.bluestone.scienceexplorer.dataclasses.SharedLink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class SharedLinkEntity(
    @Id var dbId: Long = 0,
    val name: String,
    val url: String,
    val type: String
)

fun SharedLinkEntity.toSharedLink() =
    SharedLink(
        name,
        url,
        type
    )