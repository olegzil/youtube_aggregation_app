package com.bluestone.scienceexplorer.network

data class ServerErrorResponse(
    val status: String,
    val error_text: String,
    val error_code: Int,
    val date_time: String
)
