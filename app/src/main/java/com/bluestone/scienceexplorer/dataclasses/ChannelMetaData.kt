package com.bluestone.scienceexplorer.dataclasses

import com.bluestone.scienceexplorer.database.ChannelMetadataEntity

data class ChannelMetaData(
    val channelID: String,
    val name: String,
    val lastRefreshTime: Long,
    val channelItemCount: Long
    )
fun ChannelMetaData.toChannelMetadataEntity() = ChannelMetadataEntity(
    channelID = channelID,
    name =  name,
    lastUpdateTime = lastRefreshTime,
    channelItemCount = channelItemCount
)