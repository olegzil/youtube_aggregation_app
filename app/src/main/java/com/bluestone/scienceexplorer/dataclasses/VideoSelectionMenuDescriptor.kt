package com.bluestone.scienceexplorer.dataclasses

data class VideoSelectionMenuDescriptor(
    val menuId: Int = 0,
    val menuDescription: String,
    val clientID: String,
    val channelID: String,
    val callback: (String) -> Unit
)

