package com.bluestone.scienceexplorer.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectedChannel(val clientID: String, val channelID: String, val channelTitle: String, var channelItem: ChannelItem?=null) : Parcelable
