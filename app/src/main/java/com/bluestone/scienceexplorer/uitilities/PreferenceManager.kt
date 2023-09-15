package com.bluestone.scienceexplorer.uitilities

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.bluestone.scenceexplorer.BuildConfig
import com.bluestone.scenceexplorer.R
import com.bluestone.scienceexplorer.constants.ACTIVE_CHANNEL_KEY
import java.util.*

class PreferenceManager(context: Context) {
    private val settings: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val defaultTitle = context.resources.getString(R.string.selection_screen_title)
    private val lastYoutubeAccess = "__youtube_quata__"
    private val recyclerViewLastPosition = "__last_recycler_view_position__"
    private val lastChannelID = "__last_client_id__"
    private val title = "__title__"
    private val clientIDKey = "__client_id_key__"
    private val videoCountKey = "__video_count_key__"
    private val sharingStatusKey = "__sharing_status__"
    private val channelListModifiedKey = "__channel_list_modified__"
    private val sharedLinkLastPosKey = "__shared_link_last_pos__"
    private val calendar = Calendar.getInstance(TimeZone.getDefault())
    fun getYoutubeQuotaState(): Boolean {
        val year: Int = calendar.get(Calendar.YEAR)
        val month: Int = calendar.get(Calendar.MONTH) + 1
        val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
        val date = "%d-%d-%d".format(year, month, day)
        val time = dateToLong(date)
        val lastAccess = settings.getLong(lastYoutubeAccess, 0)
        if (lastAccess == time)
            return false
        val editor = settings.edit()
        editor.putLong(lastYoutubeAccess, time)
        editor.apply()
        return true
    }
    fun putRecyclerPosition(position: Int) {
        settings.edit()
            .putInt(recyclerViewLastPosition, position)
            .apply()
    }
    fun getRecyclerPosition() =
        settings.getInt(recyclerViewLastPosition, 0)

    fun putChannelID(channelID: String){
        settings.edit()
            .putString(lastChannelID, channelID)
            .apply()
    }
    fun getChannelID() =
        settings.getString(lastChannelID, ACTIVE_CHANNEL_KEY)!!

    fun putClientID(clientID: String) {
        settings.edit()
            .putString(clientIDKey, clientID)
            .apply()
    }
    fun getTitle() =
        settings.getString(title, defaultTitle)!!
    fun putTitle(newTitle: String) {
        settings.edit()
            .putString(title, newTitle)
            .apply()
    }
    fun putVideoCount(count: Long) {
        settings.edit()
            .putLong(videoCountKey, count)
            .apply()
    }
    fun getVideoCount() = settings.getLong(videoCountKey, 0)
    fun putSharingStatus(flag: Boolean) {
        settings.edit()
            .putBoolean(sharingStatusKey, flag)
            .apply()
    }
    fun getSharingStatus() = settings.getBoolean(sharingStatusKey, false)

    fun getIsChannelListModified() = settings.getBoolean(channelListModifiedKey, true)
    fun putIsChannelListModified(flag: Boolean) {
        settings.edit()
            .putBoolean(channelListModifiedKey, flag)
            .apply()
    }

    fun getLastPositionSharedLink() = settings.getInt(sharedLinkLastPosKey, 0)
    fun putLastPositionSharedLink(pos: Int) {
        settings.edit()
            .putInt(sharedLinkLastPosKey, pos)
            .apply()
    }
}