package com.bluestone.scienceexplorer.network

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.Calendar

class YoutubeAPIDeserializer<T>(private val classType: Type) :
    JsonDeserializer<ServerResponse<T>> {
    private val parseError = ServerErrorResponse(
        "error",
        "Server sent invalid Json",
        0,
        Calendar.getInstance().time.toString()
    )

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ServerResponse<T> {
        return json?.let { jsonElement ->
            val jsonObject = jsonElement.asJsonObject
            val gson = Gson()
            when {
                jsonObject.has("success") -> ServerResponse(
                    gson.fromJson(
                        jsonObject,
                        classType,
                    ), null
                )

                jsonObject.has("videos") -> ServerResponse(
                    gson.fromJson(
                        jsonObject,
                        classType,
                    ), null
                )

                jsonObject.has("directoryrecords") -> ServerResponse(
                    gson.fromJson(
                        jsonObject,
                        classType
                    ), null
                )

                jsonObject.has("latest_video") -> ServerResponse(
                    gson.fromJson(
                        jsonObject,
                        classType
                    ), null
                )

                jsonObject.has("video_link_status") -> ServerResponse(
                    gson.fromJson(
                        jsonObject,
                        classType
                    ), null
                )

                jsonObject.has("status") -> {
                    val status = jsonObject["status"].asString
                    if (status == "error") {
                        val error = gson.fromJson(
                            jsonObject,
                            ServerErrorResponse::class.java
                        )
                        ServerResponse(
                            null,
                            error
                        )
                    } else {
                        ServerResponse(
                            gson.fromJson(
                                jsonObject,
                                classType
                            ), null
                        )
                    }
                }

                jsonObject.has("error") -> {
                    val errorBody = jsonObject["error"]
                    val error = gson.fromJson(
                        errorBody,
                        ServerErrorResponse::class.java
                    )
                    ServerResponse(
                        null,
                        error
                    )
                }

                else -> {
                    val error = ServerErrorResponse(
                        jsonObject.toString(),
                        "error",
                        -1,
                        Calendar.getInstance().time.toString()
                    )
                    ServerResponse(null, error)
                }
            }
        } ?: ServerResponse(null, parseError)
    }
}