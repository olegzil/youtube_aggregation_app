package com.bluestone.scienceexplorer.dataclasses

import com.bluestone.scienceexplorer.network.ServerResponse

data class MenuFetchResult(val success: Boolean, val menuItems: Map<Int, VideoSelectionMenuDescriptor>, val serverResponse: ServerResponse<VideoChannelDirectory>?=null)
