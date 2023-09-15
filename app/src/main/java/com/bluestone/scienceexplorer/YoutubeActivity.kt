package com.bluestone.scienceexplorer

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.bluestone.embededyoutubeplayer.PlayerView
import com.bluestone.embededyoutubeplayer.QualityControl
import com.bluestone.embededyoutubeplayer.YoutubeParameters
import com.bluestone.scenceexplorer.BuildConfig
import com.bluestone.scenceexplorer.R
import com.bluestone.scienceexplorer.constants.INTENT_KEY_YOUTUBE_DATA
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.dataclasses.VideoThumbnailItem
import com.bluestone.scienceexplorer.uitilities.parcelable
import timber.log.Timber

class YoutubeActivity : Activity() {
    private val TAG = "YTACTIVITY"
    private lateinit var mYouTubePlayerView: PlayerView
    override fun onCreate(p0: Bundle?) {
        super.onCreate(p0)
        setContentView(R.layout.youtube_activity)
        mYouTubePlayerView = findViewById(R.id.player)
        val params = YoutubeParameters()
        params.setVolume(100)
        params.setPlaybackQuality(QualityControl.hd1080)
        val data = intent?.parcelable<VideoThumbnailItem>(INTENT_KEY_YOUTUBE_DATA)
        data?.let { selectedChannel ->
            val callback = object : PlayerView.YouTubeListener {
                override fun onReady() {
                    Timber.tag(TAG).d("onRead")
                }

                override fun onStateChange(state: PlayerView.STATE?) {
                    Timber.tag(TAG).d("onStateChange")
                }

                override fun onPlaybackQualityChange(arg: String?) {
                    Timber.tag(TAG).d("onPlaybackQualityChange")
                }

                override fun onPlaybackRateChange(arg: String?) {
                    Timber.tag(TAG).d("onPlaybackRateChange")
                }

                override fun onError(arg: String?) {
                    Timber.tag(TAG).d("error: %s", arg)
                }

                override fun onApiChange(arg: String?) {
                    Timber.tag(TAG).d("onApiChange")
                }

                override fun onCurrentSecond(second: Double) {
                    Timber.tag(TAG).d("onCurrentSecond")
                }

                override fun onDuration(duration: Double) {
                    Timber.tag(TAG).d("onDuration")
                }

                override fun logs(log: String?) {
                    Timber.tag(TAG).d("Log: %s", log)
                }
            }
            mYouTubePlayerView.playFullscreen()
            mYouTubePlayerView.initializeWithCustomURL(
                selectedChannel.videoID + "?rel=0",
                params,
                callback
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mYouTubePlayerView.destroy()
    }

    override fun onPause() {
        super.onPause()
        mYouTubePlayerView.pause()
    }
}