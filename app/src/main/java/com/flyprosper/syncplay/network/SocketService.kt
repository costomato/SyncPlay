package com.flyprosper.syncplay.network

import com.flyprosper.syncplay.network.model.MessageData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

interface SocketService {
    suspend fun initSession(): Boolean
    suspend fun sendData(messageData: MessageData)
    fun observeSocket(): Flow<MessageData>
    suspend fun exitRoom(messageData: MessageData)

    companion object {
        fun create(): SocketService {
            return SocketServiceImpl(
                client = HttpClient(CIO) {
                    install(WebSockets) {
                        contentConverter =
                            KotlinxWebsocketSerializationConverter(Json) // For development
//                        contentConverter = KotlinxWebsocketSerializationConverter(ProtoBuf) // For build
                    }
                    install(Logging) {
                        level = LogLevel.ALL
                    }
                }
            )
        }
    }
}