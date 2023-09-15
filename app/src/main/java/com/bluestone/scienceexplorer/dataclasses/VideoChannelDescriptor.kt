package com.bluestone.scienceexplorer.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelItem(val date: String = "",
                       val url_medium: String = "",
                       val width: Int = 0,
                       val title: String = "",
                       val channel_id: String = "",
                       val video_id: String = "",
                       val height: Int = 0): Parcelable

data class VideoChannelDescriptor(
    val videos: List<ChannelItem> = listOf(),
)
