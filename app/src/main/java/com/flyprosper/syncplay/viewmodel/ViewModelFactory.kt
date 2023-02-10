package com.flyprosper.syncplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.flyprosper.syncplay.network.SocketService

class ViewModelFactory(
    private val service: SocketService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SocketViewModel(service) as T
    }
}