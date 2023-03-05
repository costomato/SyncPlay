package com.flyprosper.syncplay.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyprosper.syncplay.adapters.chat.ChatAdapter
import com.flyprosper.syncplay.model.Video
import com.flyprosper.syncplay.network.SocketService
import com.flyprosper.syncplay.network.model.MessageData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*

class SocketViewModel(private val socketService: SocketService) : ViewModel() {

//    private val _dataRes =
//        MutableStateFlow(MessageData(channel = "test", message = "test"))
//    val dataRes: StateFlow<MessageData> = _dataRes

//    private val _dataRes =
//        MutableSharedFlow<MessageData>()
//    val dataRes = _dataRes.asSharedFlow()

    private val _dataRes = Channel<MessageData>(Channel.BUFFERED)
    val dataRes = _dataRes.receiveAsFlow()

    var socketResult = false

    var myName = "Me"
    var roomCode: String? = null
    var videoInRoom: Video? = null
    var nUsers = 0
    var amICreator = false

    lateinit var chatAdapter: ChatAdapter
    val chats: Stack<MessageData> by lazy { Stack() }
    var intendVideoAction = false
//    var player: ExoPlayer? = null

    fun initSession() {
        viewModelScope.launch {
            socketResult = socketService.initSession()
            Log.e("initSession", "Socket active: $socketResult")
            if (socketResult) {
                socketService.observeSocket()
//                    .collectLatest {
//                        Log.e("observeSocket", "Something received: $it")
//                        Log.e("SVM emit", "_dataRes = ${_dataRes.asSharedFlow()}")
//                        Log.e("SVM emit", "dataRes = $dataRes")
//                        _dataRes.emit(it)
//                    }
//                    .onEach { data ->
//                        Log.e("observeSocket", "Something received: $data")
//                        _dataRes.value = data
//                    }.launchIn(viewModelScope)
//                    .catch {
//                        _dataRes.send(
//                            MessageData(
//                                "error",
//                                message = "Caught error in socket service",
//                                err = true
//                            )
//                        )
//                    }
                    .collectLatest {
                        Log.e("SVMObserveSocket", "Something received: $it")
                        _dataRes.send(it)
                    }
            } else {
                Log.e("SocketViewModel", "Unknown error: Could not initialize socket session")
            }
        }
    }

    fun sendData(data: MessageData) {
        Log.e("sendData", "$data")
        viewModelScope.launch {
            socketService.sendData(data)
        }
    }
}