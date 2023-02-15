package com.flyprosper.syncplay.view.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.flyprosper.syncplay.databinding.ActivityHomeBinding
import com.flyprosper.syncplay.network.SocketService
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.flyprosper.syncplay.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    lateinit var socketViewModel: SocketViewModel
    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = ViewModelFactory(SocketService.create())
        socketViewModel = ViewModelProvider(this, viewModelFactory)[SocketViewModel::class.java]
        if (savedInstanceState == null)
            socketViewModel.initSession()

        requestStoragePermission()
    }

    private fun requestStoragePermission() {
        val readVideoPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, readVideoPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    readVideoPermission
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Snackbar.make(binding.root, "Permission granted", Snackbar.LENGTH_SHORT)
                        .show()
//                    stopService(this.intent)
//                    startActivity(Intent(this, this.javaClass))
                } else {
                    Snackbar.make(binding.root, "Permission denied", Snackbar.LENGTH_SHORT)
                        .show()
                }
                return
            }
        }
    }

}