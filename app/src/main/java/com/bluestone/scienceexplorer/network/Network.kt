package com.bluestone.scienceexplorer.network

import com.bluestone.scienceexplorer.constants.ServerRequestTimeOut
import com.bluestone.scienceexplorer.constants.YOUTUBE_BASE_LINK_URL
import com.bluestone.scienceexplorer.constants.YOUTUBE_BASE_URL
import com.bluestone.scienceexplorer.constants.YOUTUBE_PLAYLIST_BASE_URL
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDescriptor
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDirectory
import com.bluestone.scienceexplorer.dataclasses.VideoChannelImage
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object Network {
    private val interceptor = HttpLoggingInterceptor()
    private fun createInterceptor(): OkHttpClient {
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient.Builder()
            .connectTimeout(ServerRequestTimeOut, TimeUnit.SECONDS)
            .readTimeout(ServerRequestTimeOut, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
    }
    fun videoPlayListAPI(): GenericUrlFetchAPI {
        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_PLAYLIST_BASE_URL)
            .addConverterFactory(HtmlConverterFactory.create(YOUTUBE_PLAYLIST_BASE_URL))
            .build()
        return retrofit.create(GenericUrlFetchAPI::class.java)
    }


    //TODO: Add an admin call to issue 'restoredefaultclient' from a Menu selection. Also, add a help selection to the menu.
    fun videoChannelAPI(): VideoChannelAPI {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                ServerResponse::class.java, YoutubeAPIDeserializer<VideoChannelDescriptor>(
                    VideoChannelDescriptor::class.java
                )
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .client(createInterceptor())
            .build()
        return retrofit.create(VideoChannelAPI::class.java)
    }

    fun deleteChannelAPI(): DeleteChannelAPI {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                ServerResponse::class.java, YoutubeAPIDeserializer<SimpleServerResponse>(
                    SimpleServerResponse::class.java
                )
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .client(createInterceptor())
            .build()
        return retrofit.create(DeleteChannelAPI::class.java)
    }

    fun latestVideoAPI(): LatestVideoAPI {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                ServerResponse::class.java, YoutubeAPIDeserializer<VideoChannelImage>(
                    VideoChannelImage::class.java
                )
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .client(createInterceptor())
            .build()
        return retrofit.create(LatestVideoAPI::class.java)
    }

    fun videoChannelDirectoryAPI(): VideoChannelDirectoryAPI {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                ServerResponse::class.java,
                YoutubeAPIDeserializer<VideoChannelDirectory>(VideoChannelDirectory::class.java)
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .client(createInterceptor())
            .build()
        return retrofit.create(VideoChannelDirectoryAPI::class.java)
    }

    fun manageVideoLinkAPI(): ManageVideoLinkAPI {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                ServerResponse::class.java,
                YoutubeAPIDeserializer<SimpleServerResponse>(SimpleServerResponse::class.java)
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .client(createInterceptor())
            .build()
        return retrofit.create(ManageVideoLinkAPI::class.java)
    }
}