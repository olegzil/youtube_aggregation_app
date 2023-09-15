package com.bluestone.scienceexplorer.dataclasses

data class ChannelSelectionMenuDescriptor(
    val menuId: Int = 0,
    var isDisabled: Boolean = false,
    val menuDescription: String,
    val clientID: String,
    val callback: (String) -> Unit
)
