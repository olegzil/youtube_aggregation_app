package com.bluestone.scienceexplorer.dataclasses

import java.sql.Timestamp

data class ErrorDescriptor(
    val domain: String,
    val message: String,
    val reason: String
)
data class Error(
    val message: String,
    val timestamp: String
)

open class YoutubeErrorResult(
    val error: Error? = null
)