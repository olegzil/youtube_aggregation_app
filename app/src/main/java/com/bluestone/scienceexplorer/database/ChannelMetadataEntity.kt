package com.bluestone.scienceexplorer.database

import com.bluestone.scienceexplorer.dataclasses.ChannelMetaData
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class ChannelMetadataEntity(
    @Id var dbId: Long = 0,
    val lastUpdateTime: Long,
    val channelID: String,
    val name: String,
    val channelItemCount: Long
    )
fun ChannelMetadataEntity.toChannelMetadata() =
    ChannelMetaData(
        channelID,
        name,
        lastUpdateTime,
        channelItemCount
    )
