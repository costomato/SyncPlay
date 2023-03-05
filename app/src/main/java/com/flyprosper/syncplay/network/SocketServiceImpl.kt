package com.flyprosper.syncplay.network

import android.util.Log
import com.flyprosper.syncplay.network.model.MessageData
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SocketServiceImpl(private val client: HttpClient) : SocketService {
    private var socket: DefaultClientWebSocketSession? = null

    override suspend fun initSession(): Boolean {
        return try {
            socket = client.webSocketSession {
                url("wss://syncplay-backend.onrender.com/ws")
//                url("ws://192.168.0.112:8080/ws")
            }
            socket?.isActive == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun sendData(messageData: MessageData) {
        try {
//            socket?.sendSerialized(messageData)
            socket?.send(messageData.convertToFrame())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun observeSocket(): Flow<MessageData> {
        return try {
            Log.e("observeSocket", "${socket?.incoming}")

            socket?.incoming?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.map {
                    it.receiveAsMessageData()
                } ?: throw Exception("Received null")

            /*flowOf(
                socket?.receiveDeserialized<MessageData>() ?: MessageData(
                    channel = "error",
                    message = "Received null as MessageData"
                )
            )*/
        } catch (e: Exception) {
            e.printStackTrace()
            flowOf(
                MessageData(
                    channel = "error",
                    message = "Received unknown data",
                    err = true
                )
            )
        }
    }

    override suspend fun exitRoom(messageData: MessageData) {
        socket?.close()
    }

    private fun Frame.receiveAsMessageData(): MessageData = Json.decodeFromString((this as? Frame.Text)?.readText() ?: "")
    private fun MessageData.convertToFrame(): Frame.Text = Frame.Text(Json.encodeToString(this))
}