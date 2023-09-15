package com.bluestone.scienceexplorer.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.dataclasses.FetchResponse
import com.bluestone.scienceexplorer.dataclasses.VideoChannelDescriptor
import com.bluestone.scienceexplorer.dataclasses.VideoThumbnailItem
import com.bluestone.scienceexplorer.dataclasses.YoutubeErrorResult
import timber.log.Timber

class VideoFragmentViewModel : ViewModel() {
    private fun extractDataAndNotifyReceiver(
        response: VideoChannelDescriptor,
        retVal: FetchResponse<VideoThumbnailItem, YoutubeErrorResult>
    ) {
        response.videos.forEach { item ->
            if (item.video_id == null)
                Timber.tag(TAG).i("Missing VideoID: %s", item.title)
            else
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
}
