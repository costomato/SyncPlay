package com.flyprosper.syncplay.view.home

import android.os.Build.VERSION
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.flyprosper.syncplay.databinding.ActivityHomeBinding
import com.flyprosper.syncplay.network.SocketService
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.flyprosper.syncplay.viewmodel.ViewModelFactory

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    lateinit var socketViewModel: SocketViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = ViewModelFactory(SocketService.create())
        socketViewModel = ViewModelProvider(this, viewModelFactory)[SocketViewModel::class.java]
        if (savedInstanceState == null)
            socketViewModel.initSession()
    }
}