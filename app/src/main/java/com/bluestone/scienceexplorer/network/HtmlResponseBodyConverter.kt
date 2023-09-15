package com.bluestone.scienceexplorer.network

import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Converter
import java.io.IOException


class HtmlResponseBodyConverter(private val baseUrl: String) :
    Converter<ResponseBody, Document> {
    @Throws(IOException::class)
    override fun convert(response: ResponseBody): Document {
        return response.use { value ->
            Jsoup.parse(value.byteStream(), "UTF-8", baseUrl)
        }
    }
}