package com.bluestone.scienceexplorer.network

import com.bluestone.scenceexplorer.BuildConfig
import com.bluestone.scienceexplorer.constants.SERVER_DELETE_CHANNEL
import com.bluestone.scienceexplorer.constants.SERVER_LINK_STATUS
import com.bluestone.scienceexplorer.constants.SERVER_REQUEST_CLIENT_DIRECTORY
import com.bluestone.scienceexplorer.constants.SERVER_RESTORE_DEFAULT_CHANNELS
import com.bluestone.scienceexplorer.constants.SERVER_ADD_VIDEO_LINK
import com.bluestone.scienceexplorer.constants.SERVER_FETCH_VIDEO_DATA
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDescriptor
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDirectory
import com.bluestone.scienceexplorer.dataclasses.VideoChannelImage
import com.bluestone.scienceexplorer.dataclasses.VideoLinkDescriptor
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url


interface GenericUrlFetchAPI {
    @Headers(
        "User-Agent:Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36, Accept-Encoding:gzip,deflate"
    )
    @GET
    fun fetchUrl(
        @Url playList: String
    ): retrofit2.Call<org.jsoup.nodes.Document>
}

interface VideoChannelAPI {
    @GET("youtube/channelselector/")
    fun channelDataAction(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("client_id") client_id: String,
        @Query("channel_id") channel_id: String,
        @Query("action") action: String
    ): retrofit2.Call<ServerResponse<VideoChannelDescriptor>>
}

interface DeleteChannelAPI {
    @GET("youtube/channelselector/")
    fun deleteChannel(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("client_id") client_id: String,
        @Query("channel_id") channel_id: String,
        @Query("action") action: String = SERVER_DELETE_CHANNEL
    ): retrofit2.Call<ServerResponse<SimpleServerResponse>>
}

interface VideoChannelDirectoryAPI {
    @GET("youtube/channelselector/")
    fun fetchVideoDirectory(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("client_id") client_id: String,
        @Query("action") action: String = SERVER_REQUEST_CLIENT_DIRECTORY
    ): retrofit2.Call<ServerResponse<VideoChannelDirectory>>

    @GET("youtube/channelselector/")
    fun restoreChannels(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("client_id") client_id: String,
        @Query("action") action: String = SERVER_RESTORE_DEFAULT_CHANNELS,
    ): retrofit2.Call<ServerResponse<VideoChannelDirectory>>
}

interface LatestVideoAPI {
    @GET("youtube/channelselector/")
    fun fetchChannelImage(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("action") action: String = "fetchchannelimage",
        @Query("channel_id") channel_id: String,
        @Query("client_id") client_id: String
    ): retrofit2.Call<ServerResponse<VideoChannelImage>>
}

interface ManageVideoLinkAPI {
    @GET("youtube/channelselector/")
    fun addVideoLink(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("action") action: String = SERVER_ADD_VIDEO_LINK,
        @Query("client_id") client_id: String,
        @Query("video_link") video_link: String,
        @Query("channel_id") channel_id: String,
        @Query("channel_name") channel_name: String

    ): retrofit2.Call<ServerResponse<SimpleServerResponse>>
    @GET("youtube/channelselector/")
    fun fetchLinkStatus(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("action") action: String = SERVER_LINK_STATUS,
        @Query("client_id") client_id: String,
        @Query("video_link") video_link: String
    ): retrofit2.Call<ServerResponse<SimpleServerResponse>>
}

interface VideoLinkStatusInterface {
    @GET("youtube/channelselector/")
    fun fetchVideoData(
        @Query("key") key: String = BuildConfig.youtube_client_key,
        @Query("action") action: String = SERVER_FETCH_VIDEO_DATA,
        @Query("client_id") client_id: String,
        @Query("video_link") video_link: String
    ): retrofit2.Call<ServerResponse<VideoLinkDescriptor>>
}