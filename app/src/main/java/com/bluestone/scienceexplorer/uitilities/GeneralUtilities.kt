package com.bluestone.scienceexplorer.uitilities

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import com.bluestone.scenceexplorer.R
import com.bluestone.scienceexplorer.MainActivity
import com.bluestone.scienceexplorer.constants.*
import com.bluestone.scienceexplorer.dataclasses.VideoWallMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> {
        @Suppress("DEPRECATION") getParcelableExtra(key)
    }
}

fun dateToLong(date: String): Long {
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return df.parse(date)?.time ?: 0L
}

fun loadData(stream: InputStream): String {
    val size: Int = stream.available()
    val buffer = ByteArray(size)
    stream.read(buffer)
    stream.close()
    return String(buffer)
}
fun computeThumbMetrics(resources: Resources): VideoWallMetrics {
    val displayMetrics = resources.displayMetrics
    val maxAllowedNumberOfRows = floor(
        (displayMetrics.heightPixels / displayMetrics.density / PLAYER_VIEW_MINIMUM_HEIGHT_DP).toDouble()
    ).toInt()
    val numberOfRows = maxAllowedNumberOfRows.coerceAtMost(MAX_NUMBER_OF_ROWS_WANTED)
    val interImagePaddingPx: Int =
        displayMetrics.density.toInt() * INTER_IMAGE_PADDING_DP
    val imageHeight = displayMetrics.heightPixels / numberOfRows - interImagePaddingPx
    val imageWidth: Int =
        (imageHeight * THUMBNAIL_ASPECT_RATIO).toInt()
    return VideoWallMetrics(
        maxAllowedNumberOfRows,
        numberOfRows,
        interImagePaddingPx,
        imageHeight,
        imageWidth
    )
}
fun cleanupName(nameIn: String) : String {
    val workName = nameIn
                        .replace("&#39;", "'")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .split("_")
    return workName.joinToString(separator = " ") { word -> word.replaceFirstChar { it.uppercase() } }
}

fun clearTitle(scope: CoroutineScope, activity: AppCompatActivity) {
    scope.launch {
        MainActivity.titleBarControl.send(false)
        activity.supportActionBar?.title = ""
        MainActivity.titleBarControl.send(true)
    }
}
fun updateTitle(activity: AppCompatActivity, newTitle: String, count: Long) {
    val title = "%s: %d".format(newTitle, count)
    activity.supportActionBar?.title = title
}

fun translateServerError(resource:Resources, code: Int, message: String=" ") =
    when (code) {
        0xFFAA00 -> resource.getString(R.string.server_response_ok)
        0xFFAA01 -> resource.getString(R.string.server_error_client_id_is_invalid).format(message)
        0xFFAA02 -> resource.getString(R.string.server_error_missing_action_token)
        0xFFAA03 -> resource.getString(R.string.server_error_missing_parameter)
        0xFFAA04 -> resource.getString(R.string.server_error_malformed_command)
        0xFFAA05 -> resource.getString(R.string.server_error_invalid_command)
        0xFFAA06 -> resource.getString(R.string.server_error_database_error)
        0xFFAA07 -> resource.getString(R.string.server_error_not_found)
        0xFFAA08 -> resource.getString(R.string.server_error_authentication_error)
        0xFFAA09 -> resource.getString(R.string.server_error_invalid_action)
        0xFFAA0A -> resource.getString(R.string.server_error_missing_channel)
        0xFFAA0B -> resource.getString(R.string.server_error_missing_client)
        0xFFAA0C -> resource.getString(R.string.server_error_invalid_client_id)
        0xFFAA0D -> resource.getString(R.string.server_error_missing_video)
        0xFFAB00 -> resource.getString(R.string.server_error_invalid_link)
        0xFFAB01 -> resource.getString(R.string.link_message_duplicate).format((message))
        0xFFAB02 -> resource.getString(R.string.server_error_link_pending).format(message)
        0xFFAB03 -> resource.getString(R.string.server_error_link_added).format(message)
        0xFFAB04 -> resource.getString(R.string.server_error_unable_to_add_link)
        0xFFAA11 -> resource.getString(R.string.server_error_record_not_found)
        0xFFAA12 -> resource.getString(R.string.server_error_invalid_admin_key)
        0xFFAA13 -> resource.getString(R.string.server_error_no_action_specified)
        0xFFAA14 -> resource.getString(R.string.server_error_invalid_action_type)
        0xFFAA15 -> resource.getString(R.string.server_error_no_such_client)
        0xFFAA16 -> resource.getString(R.string.server_error_invalid_parameter)
        0xFFAA17 -> resource.getString(R.string.server_error_not_implemented)
        0xFFAA18 -> resource.getString(R.string.server_error_no_such_channel)
        0xFFAA19 -> resource.getString(R.string.server_error_no_such_command)
        0xFFAA1A -> resource.getString(R.string.server_error_duplicate_channel)
        else -> resource.getString(R.string.server_error_unknown)
    }
