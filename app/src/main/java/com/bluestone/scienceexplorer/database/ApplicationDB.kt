package com.bluestone.scienceexplorer.database

import com.bluestone.scienceexplorer.dataclasses.SharedLink
import com.bluestone.scienceexplorer.interfaces.YoutubeDBInterface
import io.objectbox.Box

class ApplicationDB : YoutubeDBInterface {
    private val applicationDatabase: Box<ApplicationControlEntity> =
        ObjectBox.get().boxFor(ApplicationControlEntity::class.java)

    private val channelMetadataDatabase: Box<ChannelMetadataEntity> =
        ObjectBox.get().boxFor(ChannelMetadataEntity::class.java)

    private val sharedLinkDatabase: Box<SharedLinkEntity> =
        ObjectBox.get().boxFor(SharedLinkEntity::class.java)

    override fun getChannelMetaData(channelID: String): List<ChannelMetadataEntity> =
        channelMetadataDatabase.query(ChannelMetadataEntity_.channelID.equal(channelID)).build()
            .find()


    override fun putChannelMetaData(item: ChannelMetadataEntity) {
        val result =
            channelMetadataDatabase.query(ChannelMetadataEntity_.channelID.equal(item.channelID))
                .build().find()
        if (result.isEmpty())
            channelMetadataDatabase.put(item)
        else
            channelMetadataDatabase.put(
                ChannelMetadataEntity(
                    dbId = result[0].dbId,
                    lastUpdateTime = item.lastUpdateTime,
                    channelID = item.channelID,
                    name = item.name,
                    channelItemCount = item.channelItemCount
                )
            )
    }

    override fun getApplicationControl(): ApplicationControlEntity? {
        val result = applicationDatabase.query().build().find()
        return if (result.isEmpty())
            null
        else
            result[0]
    }

    override fun putApplicationControl(controlBlock: ApplicationControlEntity) {
        val result = applicationDatabase.query().build().find()
        if (result.isEmpty())
            applicationDatabase.put(controlBlock)
        else
            applicationDatabase.put(ApplicationControlEntity(controlBlock.dbId, controlBlock.defaultClientID, controlBlock.uniqueClientID, controlBlock.appVersion   ))
    }
    fun deleteSharedLink(key: String) {
        val items = sharedLinkDatabase.query(SharedLinkEntity_.url.equal((key))).build().find()
        if (items.isNotEmpty()){
            sharedLinkDatabase.remove(items[0].dbId)
        }
    }
    fun putSharedLink(sharedLink: SharedLinkEntity) {
        val result = sharedLinkDatabase.query(SharedLinkEntity_.url.equal((sharedLink.url))).build().find()
        if (result.isEmpty()){
            sharedLinkDatabase.put(sharedLink)
        } else {
            val newEnt = SharedLinkEntity(result[0].dbId, sharedLink.name, sharedLink.url, sharedLink.type)
            sharedLinkDatabase.put(newEnt)
        }
    }
    fun getSharedLink(key: String): SharedLinkEntity? =
        sharedLinkDatabase.query(SharedLinkEntity_.url.equal(key)).build().find().let {
            if (it.isEmpty()) null else it[0]
        }

    fun getAllSharedLinks(): MutableList<SharedLinkEntity>? =
        sharedLinkDatabase.query().build().find().let {
            if (it.isEmpty())
                null
            else
                it
        }
}