package com.example.androidapp_bleandwebsocket
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class WebsocketDataAdapter(val price: String?)
