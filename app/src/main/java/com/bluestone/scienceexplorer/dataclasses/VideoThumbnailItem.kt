package com.bluestone.scienceexplorer.dataclasses

import android.os.Parcelable

@kotlinx.parcelize.Parcelize
data class VideoThumbnailItem(
    val videoID: String,
    val urlMedium: String,
    val width: Int,
    val height: Int,
    var title: String,
    val date: String
) : Parcelable