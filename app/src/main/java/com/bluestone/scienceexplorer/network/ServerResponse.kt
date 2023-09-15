package com.bluestone.scienceexplorer.network

data class ServerResponse<T>(val success: T?=null, val error:ServerErrorResponse?=null)
