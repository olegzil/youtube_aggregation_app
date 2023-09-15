package com.bluestone.scienceexplorer.interfaces

import com.bluestone.scienceexplorer.database.*
import com.bluestone.scienceexplorer.database.ApplicationControlEntity

interface YoutubeDBInterface {
    fun getApplicationControl() : ApplicationControlEntity?
    fun putApplicationControl(controlBlock: ApplicationControlEntity)
    fun getChannelMetaData(channelID: String): List<ChannelMetadataEntity>
    fun putChannelMetaData(item: ChannelMetadataEntity)
}
