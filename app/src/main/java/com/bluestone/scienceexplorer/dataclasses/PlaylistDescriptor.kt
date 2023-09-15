package com.bluestone.scienceexplorer.dataclasses

data class VideoLinkDescriptor(
    val author: String,
    val title: String,
    val channel_id: String,
    val video_id: String,
    val url_medium: String,
    val date: Int,
    val width: Int = 0,
    val height: Int = 0
    )
data class PlaylistDescriptor(var title: String="", var imageURL: String="", var width: Int=0, var height: Int=0)
