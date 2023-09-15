package com.bluestone.scienceexplorer.dataclasses

data class VideoChannelDirectory(val directoryrecords: List<DirectoryrecordsItem>)
data class DirectoryrecordsItem(val latest_video: ChannelItem,
                                val name: String = "",
                                val client: String = "",
                                val channel_id: String = "")
