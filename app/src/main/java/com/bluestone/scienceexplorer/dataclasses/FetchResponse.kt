package com.bluestone.scienceexplorer.dataclasses

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

data class FetchResponse <TSuccess, TFailure>(
    val onFirstEmission: Channel<TSuccess> = Channel(Channel.CONFLATED),
    val onSuccess: Channel<TSuccess> = Channel(Channel.UNLIMITED),
    val onFailure: Channel<TFailure> = Channel(Channel.CONFLATED),
    val onComplete: Channel<Long> = Channel(Channel.CONFLATED),
    val onProgress: Channel<Int> = Channel(Channel.UNLIMITED),
    var activeJob: Job = Job()
)
