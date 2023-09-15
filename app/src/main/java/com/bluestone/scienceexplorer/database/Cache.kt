package com.bluestone.scienceexplorer.database

import android.net.Uri
import android.util.Log
import com.bluestone.scenceexplorer.BuildConfig
import com.bluestone.scienceexplorer.constants.SERVER_ADD_VIDEO_LINK
import com.bluestone.scienceexplorer.constants.SERVER_REQUEST_CHANNEL_VIDEOS
import com.bluestone.scienceexplorer.constants.SERVER_UPDATE_CHANNEL
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.constants.YOUTUBE_VIDEO_BASE_URL
import com.bluestone.scienceexplorer.dataclasses.ApplicationControlBlock
import com.bluestone.scienceexplorer.dataclasses.DirectoryrecordsItem
import com.bluestone.scienceexplorer.dataclasses.Error
import com.bluestone.scienceexplorer.dataclasses.FetchResponse
import com.bluestone.scienceexplorer.dataclasses.PlaylistDescriptor
import com.bluestone.scienceexplorer.dataclasses.SharedLink
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDescriptor
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDirectory
import com.bluestone.scienceexplorer.dataclasses.VideoChannelImage
import com.bluestone.scienceexplorer.dataclasses.VideoLinkDescriptor
import com.bluestone.scienceexplorer.dataclasses.VideoThumbnailItem
import com.bluestone.scienceexplorer.dataclasses.YoutubeErrorResult
import com.bluestone.scienceexplorer.dataclasses.toSharedLinkEntity
import com.bluestone.scienceexplorer.network.Network
import com.bluestone.scienceexplorer.network.ServerErrorResponse
import com.bluestone.scienceexplorer.network.ServerResponse
import com.bluestone.scienceexplorer.network.SimpleServerResponse
import com.bluestone.scienceexplorer.uitilities.dateToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.jsoup.nodes.Document
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object Cache {
    private val dataBase = ApplicationDB()
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    fun notifyServerOfNewVideoLink(
        clientID: String,
        videoLink: String,
        channelID: String,
        channelName: String
    ): FetchResponse<SimpleServerResponse, ServerErrorResponse> {
        val retVal = FetchResponse<SimpleServerResponse, ServerErrorResponse>()
        scope.launch {
            val result = Network.manageVideoLinkAPI().addVideoLink(
                key = BuildConfig.youtube_client_key,
                client_id = clientID,
                action = SERVER_ADD_VIDEO_LINK,
                video_link = videoLink,
                channel_id = channelID,
                channel_name = channelName
            )
            result.enqueue(object : Callback<ServerResponse<SimpleServerResponse>> {
                override fun onResponse(
                    call: Call<ServerResponse<SimpleServerResponse>>,
                    response: Response<ServerResponse<SimpleServerResponse>>
                ) {
                    response.body()?.let { serverReturn ->
                        serverReturn.success?.let { serverSimpleResponse ->
                            retVal.onSuccess.trySend(serverSimpleResponse)
                        }

                        serverReturn.error?.let { error ->
                            retVal.onFailure.trySend(error)
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<SimpleServerResponse>>,
                    t: Throwable
                ) {
                    val currentDate = Date(System.currentTimeMillis())
                    val dateAsString = currentDate.toString()
                    retVal.onFailure.trySend(
                        ServerErrorResponse(
                            status = "network error",
                            t.toString(),
                            0,
                            dateAsString
                        )
                    )
                }
            })
        }
        return retVal
    }

    fun fetchLatestChannelImage(
        channelID: String,
        clientID: String
    ): FetchResponse<VideoChannelImage, ServerErrorResponse> {
        val retVal = FetchResponse<VideoChannelImage, ServerErrorResponse>()
        scope.launch {
            val result = Network.latestVideoAPI()
                .fetchChannelImage(channel_id = channelID, client_id = clientID)
            result.enqueue(object : Callback<ServerResponse<VideoChannelImage>> {
                override fun onResponse(
                    call: Call<ServerResponse<VideoChannelImage>>,
                    response: Response<ServerResponse<VideoChannelImage>>
                ) {
                    response.body()?.let { channelDirectory ->
                        channelDirectory.success?.let {
                            retVal.onSuccess.trySend(it)
                        }
                        channelDirectory.error?.let { error ->
                            retVal.onFailure.trySend(error)
                        }
                        retVal.onComplete.trySend(
                            1
                        )
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<VideoChannelImage>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "Error: $t")
                    retVal.onFailure.trySend(
                        ServerErrorResponse(
                            "error",
                            t.toString(),
                            -1,
                            Calendar.getInstance().time.toString()
                        )
                    )
                }
            })
        }
        return retVal
    }

    private fun fetchAvailableVideoListFromServer(clientID: String): FetchResponse<DirectoryrecordsItem, ServerErrorResponse> {
        val retVal = FetchResponse<DirectoryrecordsItem, ServerErrorResponse>()
        scope.launch {
            val result = Network.videoChannelDirectoryAPI()
                .fetchVideoDirectory(key = BuildConfig.youtube_client_key, client_id = clientID)
            result.enqueue(object : Callback<ServerResponse<VideoChannelDirectory>> {
                override fun onResponse(
                    call: Call<ServerResponse<VideoChannelDirectory>>,
                    response: Response<ServerResponse<VideoChannelDirectory>>
                ) {
                    response.body()?.let { channelDirectory ->
                        channelDirectory.success?.directoryrecords?.let { list ->
                            retVal.onFirstEmission.trySend(list[0])
                        }
                        channelDirectory.success?.directoryrecords?.forEach {
                            retVal.onSuccess.trySend(it)
                        }
                        channelDirectory.error?.let { error ->
                            Log.d(TAG, "fetchAvailableVideoListFromServer:onFailure")
                            retVal.onFailure.trySend(error)
                        }
                        retVal.onComplete.trySend(
                            channelDirectory.success?.directoryrecords?.size?.toLong() ?: 0
                        )
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<VideoChannelDirectory>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "Error: $t")
                    retVal.onFailure.trySend(
                        ServerErrorResponse(
                            "error",
                            t.cause.toString(),
                            0,
                            Calendar.getInstance().time.toString()
                        )
                    )
                }
            })
        }
        return retVal
    }

    fun forceFetchAvailableVideo(
        clientID: String,
        channelID: String
    ): FetchResponse<VideoThumbnailItem, ServerErrorResponse> {
        val result = dataBase.getChannelMetaData(channelID)
        return if (result.isEmpty()) {
            dataBase.putChannelMetaData(
                ChannelMetadataEntity(
                    lastUpdateTime = System.currentTimeMillis(),
                    channelID = channelID,
                    channelItemCount = 0,
                    name = "?"
                )
            )
            fetchVideoThumbnailsRemote(channelID, clientID, SERVER_UPDATE_CHANNEL)
        } else if (TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toHours(
                result[0].lastUpdateTime
            ) >= 24
        ) {
            dataBase.putChannelMetaData(
                ChannelMetadataEntity(
                    lastUpdateTime = System.currentTimeMillis(),
                    channelID = channelID,
                    channelItemCount = 0,
                    name = "?"
                )
            )
            fetchVideoThumbnailsRemote(channelID, clientID, SERVER_UPDATE_CHANNEL)
        } else {
            fetchAllVideo(clientID, channelID)

        }
    }

    fun restoreDeletedChannels(clientID: String): FetchResponse<DirectoryrecordsItem, ServerErrorResponse> =
        restoreChannels(clientID)

    fun fetchAvailableVideoList(clientID: String) =
        fetchAvailableVideoListFromServer(clientID)

    fun fetchAllVideo(
        clientID: String,
        channelID: String
    ): FetchResponse<VideoThumbnailItem, ServerErrorResponse> {
        val retVal = FetchResponse<VideoThumbnailItem, ServerErrorResponse>()
        scope.launch {
            val remoteResult =
                fetchVideoThumbnailsRemote(channelID, clientID, SERVER_REQUEST_CHANNEL_VIDEOS)
            notificationHelper(remoteResult, retVal)
        }
        return retVal
    }

    private suspend fun notificationHelper(
        result: FetchResponse<VideoThumbnailItem, ServerErrorResponse>,
        destination: FetchResponse<VideoThumbnailItem, ServerErrorResponse>,
        callback: ((VideoThumbnailItem) -> Unit)? = null
    ) {
        while (select {
                result.onSuccess.onReceive {
                    callback?.let { caller ->
                        caller(it)
                    }
                    destination.onSuccess.send(it)
                    true
                }
                result.onComplete.onReceive {
                    destination.onComplete.trySend(it)
                    false
                }
                result.onFailure.onReceive {
                    destination.onFailure.trySend(it)
                    false
                }
            }) {/*no body*/
        }
    }

    private fun extractDirectoryDataAndNotifyReceiver(
        response: VideoChannelDirectory,
        retVal: FetchResponse<DirectoryrecordsItem, ServerErrorResponse>
    ) {
        retVal.onFirstEmission.trySend(response.directoryrecords[0])
        response.directoryrecords.forEach { item ->
            retVal.onSuccess.trySend(
                DirectoryrecordsItem(
                    item.latest_video,
                    item.name,
                    item.client,
                    item.channel_id
                )
            )
        }
        retVal.onComplete.trySend(response.directoryrecords.size.toLong())
    }

    private fun extractVideoDataAndNotifyReceiver(
        response: VideoChannelDescriptor,
        retVal: FetchResponse<VideoThumbnailItem, ServerErrorResponse>
    ) {
        val first = response.videos[0]
        retVal.onFirstEmission.trySend(
            VideoThumbnailItem(
                first.video_id,
                first.url_medium,
                first.width,
                first.height,
                first.title,
                first.date
            )
        )
        response.videos.forEach { item ->
            Log.d(TAG, "${item.title} ${item.video_id}")
            retVal.onSuccess.trySend(
                VideoThumbnailItem(
                    item.video_id,
                    item.url_medium,
                    item.width,
                    item.height,
                    item.title,
                    item.date
                )
            )
        }
        retVal.onComplete.trySend(response.videos.size.toLong())
    }

    private fun restoreChannels(channelID: String): FetchResponse<DirectoryrecordsItem, ServerErrorResponse> {
        val retVal = FetchResponse<DirectoryrecordsItem, ServerErrorResponse>()
        try {
            val call: Call<ServerResponse<VideoChannelDirectory>> =
                Network.videoChannelDirectoryAPI().restoreChannels(
                    client_id = channelID,
                )
            call.enqueue(object : Callback<ServerResponse<VideoChannelDirectory>> {
                override fun onResponse(
                    call: Call<ServerResponse<VideoChannelDirectory>>,
                    response: Response<ServerResponse<VideoChannelDirectory>>
                ) {
                    response.body()?.let { serverResponse ->
                        serverResponse.error?.let { errorResponse ->
                            retVal.onFailure.trySend(
                                errorResponse
                            )
                        } ?: kotlin.run {
                            serverResponse.success?.let { VideoChannelDirectory ->
                                extractDirectoryDataAndNotifyReceiver(VideoChannelDirectory, retVal)
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<VideoChannelDirectory>>,
                    t: Throwable
                ) {
                    YoutubeErrorResult(Error(t.toString(), "current date"))
                }

            })
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            retVal.onFailure.trySend(
                ServerErrorResponse(
                    "error",
                    e.toString(),
                    0,
                    Date(System.currentTimeMillis()).toString()
                )
            )
        }
        return retVal
    }

    private fun extractToken(
        source: String,
        token: String,
        start: Int
    ): Pair<String, String> {
        val keyStart = source.indexOf(token, start, true)
        val keyEnd = keyStart + token.length
        val valueStart = keyEnd + 3
        val valueEnd = source.indexOf("\"", valueStart)
        return if (keyStart < 0 || keyEnd < 0)
            Pair("not found", "not found")
        else
            Pair(source.substring(keyStart, keyEnd), source.substring(valueStart, valueEnd))
    }

    fun fetchVideoMetaDataFromUrl(url: String): FetchResponse<VideoLinkDescriptor, ServerErrorResponse> {
        var title = "?"
        var channelID = "?"
        var videoId = "?"
        var thumbnail = "?"
        var author = "?"
        var date = "?"

        val retVal = FetchResponse<VideoLinkDescriptor, ServerErrorResponse>()
        val path = Uri.parse(url).path
        val sourceUrl = YOUTUBE_VIDEO_BASE_URL + path!!.replace("/", "")

        Network.videoPlayListAPI().fetchUrl(sourceUrl).enqueue(object : Callback<Document> {
            override fun onResponse(call: Call<Document>, response: Response<Document>) {
                response.body()?.let { doc ->
                    var found = false
                    var index = 0
                    while (!found && index < doc.childrenSize()) {
                        val element = doc.child(index)
                        var titleStart = element.data().indexOf("author", 0, true)
                        if (titleStart >=0  && author == "?") {
                            titleStart--
                            val source = element.data().substring(titleStart)
                            val pair = extractToken(source, "author", 0)
                            val re = Regex("[^\\p{L}\\p{N}\\s]")
                            author = re.replace(pair.second, "")
                        }

                        val videoDetailsStart = element.data().indexOf("videoDetails", 0)
                        if (videoDetailsStart >= 0) {
                            val target = element.data().substring(videoDetailsStart)
                            var startPublishDate =
                                target.indexOf("publishDate", videoDetailsStart)
                            if (startPublishDate >= 0 && date == "?") {
                                startPublishDate--
                                val source = target.substring(startPublishDate)
                                date = extractToken(source, "publishDate", 0).second
                            }
                            val source = element.data().substring(videoDetailsStart)
                            var startOfVideoId = source.indexOf("videoId", 0) - 1
                            if (startOfVideoId >= 0 && videoId == "?"){
                                startOfVideoId--
                                videoId = extractToken(element.data().substring(startOfVideoId), "videoId", 0).second
                            }
                            var startOfChannelID =
                                source.indexOf("channelId", videoDetailsStart, true)
                            if (startOfChannelID != -1 && channelID =="?") {
                                startOfChannelID--
                                channelID = extractToken(element.data().substring(startOfChannelID), "channelId", 0).second
                            }
                            val startOfUrl = source.indexOf("url", videoDetailsStart, true)
                                if (startOfUrl != -1 && thumbnail =="?") {
                                    val pair = extractToken(element.data().substring(startOfChannelID), "url", 0)
                                    thumbnail = pair.second.split("\\")[0]
                            }
                            if (author != "?" && videoId != "?" && channelID != "?" && thumbnail != "?" && date != "?"){
                                found = true
                                Timber.tag(TAG)
                                    .d("author: $author videoID: $videoId channelID: $channelID thumbnail: $thumbnail date: $date")
                            }
                        }
                        index++
                    }
                    val numericDate = if (date == "?") {
                        System.currentTimeMillis()
                    } else {
                        dateToLong(date)
                    }

                    retVal.onSuccess.trySend(
                        VideoLinkDescriptor(
                            title,
                            channelID,
                            videoId,
                            thumbnail,
                            author,
                            numericDate.toInt()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<Document>, t: Throwable) {
                val currentDate = Date(System.currentTimeMillis())
                val dateAsString = currentDate.toString()
                retVal.onFailure.trySend(
                    ServerErrorResponse(
                        status = "failure",
                        error_text = t.toString(),
                        error_code = -1,
                        date_time = dateAsString
                    )
                )
            }
        })
        return retVal
    }

    fun fetchPlaylistPage(playlistID: String): FetchResponse<List<PlaylistDescriptor>, ServerErrorResponse> {
        val retVal = FetchResponse<List<PlaylistDescriptor>, ServerErrorResponse>()
        Network.videoPlayListAPI().fetchUrl(playlistID).enqueue(object : Callback<Document> {
            override fun onResponse(call: Call<Document>, response: Response<Document>) {
                response.body()?.let { document ->
                    var index = 0
                    val playListDescriptor = PlaylistDescriptor()
                    val list = mutableListOf<PlaylistDescriptor>()
                    document.body().forEach(Consumer { element ->
                        when (
                            element.getElementsByAttribute("property").attr("property")) {
                            "og:title" -> {
                                playListDescriptor.title =
                                    element.getElementsByAttribute("property").attr("content")
                            }

                            "og:image" -> {
                                playListDescriptor.imageURL =
                                    element.getElementsByAttribute("property").attr("content")
                            }

                            "og:image:width" -> {
                                playListDescriptor.width =
                                    element.getElementsByAttribute("property").attr("content")
                                        .toInt()
                            }

                            "og:image:height" -> {
                                playListDescriptor.height =
                                    element.getElementsByAttribute("property").attr("content")
                                        .toInt()
                                list.add(playListDescriptor.copy())
                                index++
                            }
                        }
                    })
                    retVal.onSuccess.trySend(list)
                    retVal.onComplete.trySend(list.size.toLong())
                }
            }

            override fun onFailure(call: Call<Document>, t: Throwable) {
                val currentDate = Date(System.currentTimeMillis())
                val dateAsString = currentDate.toString()
                retVal.onFailure.trySend(
                    ServerErrorResponse(
                        status = "failure",
                        error_text = t.toString(),
                        error_code = -1,
                        date_time = dateAsString
                    )
                )
            }
        })

        return retVal
    }

    private fun fetchVideoThumbnailsRemote(
        channelID: String,
        clientID: String,
        action: String
    ): FetchResponse<VideoThumbnailItem, ServerErrorResponse> {
        val retVal = FetchResponse<VideoThumbnailItem, ServerErrorResponse>()
        try {
            val call: Call<ServerResponse<VideoChannelDescriptor>> =
                Network.videoChannelAPI().channelDataAction(
                    channel_id = channelID,
                    client_id = clientID,
                    action = action
                )
            call.enqueue(object : Callback<ServerResponse<VideoChannelDescriptor>> {
                override fun onResponse(
                    call: Call<ServerResponse<VideoChannelDescriptor>>,
                    response: Response<ServerResponse<VideoChannelDescriptor>>
                ) {
                    response.body()?.let { serverResponse ->
                        serverResponse.error?.let { errorResponse ->
                            retVal.onFailure.trySend(
                                errorResponse
                            )
                        } ?: kotlin.run {
                            serverResponse.success?.let { videoChannelDescriptor ->
                                extractVideoDataAndNotifyReceiver(videoChannelDescriptor, retVal)
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<VideoChannelDescriptor>>,
                    t: Throwable
                ) {
                    YoutubeErrorResult(Error(t.toString(), "current date"))
                }

            })
        } catch (e: Exception) {
            Timber.tag(TAG).e(e.toString())
            retVal.onFailure.trySend(
                ServerErrorResponse(
                    "error",
                    e.toString(),
                    0,
                    Date(System.currentTimeMillis()).toString()
                )
            )
        }
        return retVal
    }

    fun fetchLinkStatus(
        clientID: String,
        videoLink: String
    ): FetchResponse<SimpleServerResponse, ServerErrorResponse> {
        val retVal = FetchResponse<SimpleServerResponse, ServerErrorResponse>()
        scope.launch {
            val result = Network.manageVideoLinkAPI()
                .fetchLinkStatus(
                    key = BuildConfig.youtube_client_key,
                    client_id = clientID,
                    video_link = videoLink
                )
            result.enqueue(object : Callback<ServerResponse<SimpleServerResponse>> {
                override fun onResponse(
                    call: Call<ServerResponse<SimpleServerResponse>>,
                    response: Response<ServerResponse<SimpleServerResponse>>
                ) {
                    response.body()?.let { serverResponse ->
                        serverResponse.success?.let {
                            retVal.onSuccess.trySend(it)
                        }
                        serverResponse.error?.let { error ->
                            Timber.tag(TAG).d("fetchLinkStatus:onFailure")
                            retVal.onFailure.trySend(error)
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<SimpleServerResponse>>,
                    t: Throwable
                ) {
                    Timber.tag(TAG).d(t.toString(), "Error: %s")
                    retVal.onFailure.trySend(
                        ServerErrorResponse(
                            "error",
                            t.cause.toString(),
                            0,
                            Calendar.getInstance().time.toString()
                        )
                    )
                }
            })
        }
        return retVal
    }

    private fun putUniqueClientID(clientID: String) {
        dataBase.getApplicationControl()?.let { applicationControlEntity ->
            dataBase.putApplicationControl(
                ApplicationControlEntity(
                    applicationControlEntity.dbId,
                    applicationControlEntity.defaultClientID,
                    clientID,
                    applicationControlEntity.appVersion
                )
            )
        } ?: kotlin.run {
            throw Throwable("Application control block does not exist")
        }
    }

    fun getUniqueClientID() =
        dataBase.getApplicationControl()?.uniqueClientID ?: kotlin.run {
            throw Throwable("Application control block does not exist")
        }

    fun putApplicationControlBlock(controlBlock: ApplicationControlBlock) {
        dataBase.getApplicationControl()?.let { applicationControlEntity ->
            dataBase.putApplicationControl(
                ApplicationControlEntity(
                    applicationControlEntity.dbId,
                    controlBlock.defaultClientID,
                    controlBlock.uniqueClientID,
                    controlBlock.appVersion
                )
            )
        } ?: kotlin.run {
            dataBase.putApplicationControl(
                ApplicationControlEntity(
                    0,
                    controlBlock.defaultClientID,
                    controlBlock.uniqueClientID,
                    controlBlock.appVersion
                )
            )
        }
    }

    fun getApplicationControlBlock(): ApplicationControlBlock {
        return dataBase.getApplicationControl()?.let { controlBlock ->
            ApplicationControlBlock(
                controlBlock.defaultClientID,
                controlBlock.uniqueClientID,
                controlBlock.appVersion
            )
        } ?: kotlin.run {
            ApplicationControlBlock(
                "",
                "",
                ""
            )
        }
    }

    fun generateOrFetchClientID(): String {
        val controlBlock = getApplicationControlBlock()
        return if (controlBlock.uniqueClientID.isEmpty() || controlBlock.uniqueClientID == controlBlock.defaultClientID) {
            val uniqueClientID = UUID.randomUUID().toString()
            putUniqueClientID(uniqueClientID)
            uniqueClientID
        } else {
            controlBlock.uniqueClientID
        }
    }

    fun deleteChannel(
        clientID: String,
        channelID: String,
    ): FetchResponse<SimpleServerResponse, ServerErrorResponse> {
        val retVal = FetchResponse<SimpleServerResponse, ServerErrorResponse>()
        try {
            val call: Call<ServerResponse<SimpleServerResponse>> =
                Network.deleteChannelAPI().deleteChannel(
                    channel_id = channelID,
                    client_id = clientID
                )
            call.enqueue(object : Callback<ServerResponse<SimpleServerResponse>> {
                override fun onResponse(
                    call: Call<ServerResponse<SimpleServerResponse>>,
                    response: Response<ServerResponse<SimpleServerResponse>>
                ) {
                    response.body()?.let { serverResponse ->
                        serverResponse.error?.let { errorResponse ->
                            retVal.onFailure.trySend(
                                errorResponse
                            )
                        }
                        serverResponse.success?.let {
                            retVal.onSuccess.trySend(it)
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ServerResponse<SimpleServerResponse>>,
                    t: Throwable
                ) {
                    YoutubeErrorResult(Error(t.toString(), "current date"))
                }

            })
        } catch (e: Exception) {
            Timber.tag(TAG).e(e.toString())
            retVal.onFailure.trySend(
                ServerErrorResponse(
                    "error",
                    e.toString(),
                    0,
                    Date(System.currentTimeMillis()).toString()
                )
            )
        }
        return retVal
    }

    fun getSharedLinkCount() =
        dataBase.getAllSharedLinks()?.let {
            it.size
        } ?: kotlin.run { 0 }

    fun deleteSharedLink(key: String) {
        dataBase.deleteSharedLink(key)
    }

    fun getSharedLink(key: String): SharedLink? =
        dataBase.getSharedLink(key)?.toSharedLink()

    fun getSharedLinks(): List<SharedLink>? =
        dataBase.getAllSharedLinks()?.let { entity ->
            entity.map {
                it.toSharedLink()
            }
        }

    fun putSharedLink(link: SharedLink) =
        dataBase.putSharedLink(link.toSharedLinkEntity())
}