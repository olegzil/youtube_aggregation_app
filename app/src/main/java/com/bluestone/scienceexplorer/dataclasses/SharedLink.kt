package com.bluestone.scienceexplorer.dataclasses

import android.os.Parcelable
import com.bluestone.scienceexplorer.database.SharedLinkEntity
import kotlinx.parcelize.Parcelize
@Parcelize
data class SharedLink(val name:String, val url:String, val type: String): Parcelable

fun SharedLink.toSharedLinkEntity() =
    SharedLinkEntity(0, name, url, type)
