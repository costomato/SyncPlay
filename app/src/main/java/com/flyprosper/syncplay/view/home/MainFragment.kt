package com.flyprosper.syncplay.view.home

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flyprosper.syncplay.BuildConfig
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.adapters.library.VideoAdapter
import com.flyprosper.syncplay.databinding.FragmentMainBinding
import com.flyprosper.syncplay.dialogs.ProgressDialog
import com.flyprosper.syncplay.model.Video
import com.flyprosper.syncplay.network.model.MessageData
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoAdapter: VideoAdapter
    private lateinit var rvLibrary: RecyclerView
    private val videos: MutableList<Video> by lazy { mutableListOf() }
    private var cursor: Cursor? = null

    private lateinit var socketViewModel: SocketViewModel
    private lateinit var progressDialog: ProgressDialog

    private var socketResponseJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentMainBinding.inflate(inflater, container, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Do work here
        if (savedInstanceState == null) {
            init(view)
            loadVideos()

            initSocket(view)
        }
    }

    private fun init(view: View) {
        progressDialog = ProgressDialog(requireContext())

        binding.ivOpenFileManager.setOnClickListener {
            Navigation.findNavController(view)
                .navigate(R.id.action_mainFragment_to_fileManagerFragment)
        }

        rvLibrary = binding.rvLibrary
        rvLibrary.layoutManager =
            object : LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false) {
                override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                    lp.width = width / 2
                    return true
                }
            }

        rvLibrary.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollHorizontally(1)) {
                    loadVideos()
                }
            }
        })

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION
        )

        cursor = context?.contentResolver?.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
            null, null, null
        )

        videoAdapter = VideoAdapter(object : VideoAdapter.OnVideoClickListener {
            override fun onVideoClick(videoFile: Video) {
                if (socketViewModel.socketResult) {
                    progressDialog.show()
                    val messageData = MessageData(
                        channel = "create-room",
                        message = videoFile.title,
                        appVersion = BuildConfig.VERSION_NAME,
                        info = videoFile.duration
                    )
                    socketViewModel.videoInRoom = videoFile
                    socketViewModel.sendData(messageData)
                } else
                    Snackbar.make(
                        binding.root,
                        getString(R.string.please_wait_socket),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
            }
        })
        rvLibrary.adapter = videoAdapter

//        send join room request
        binding.tvJoinRoom.setOnClickListener {
            if (socketViewModel.socketResult) {
                val roomCode = binding.inputJoinCode.editText?.text?.trim().toString()
                if (roomCode.isNotEmpty()) {
                    progressDialog.show()
                    socketViewModel.sendData(
                        MessageData(
                            channel = "join-room",
                            roomCode = roomCode,
                            message = "Wanna join",
                            appVersion = BuildConfig.VERSION_NAME
                        )
                    )
                }
            } else
                Snackbar.make(
                    binding.root,
                    getString(R.string.please_wait_socket),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
        }
    }

    private fun loadVideos() {
        cursor?.let {
            var i = 0
            while (it.moveToNext() && i < 10) {
                i++
                val id = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE))
                val data = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val duration =
                    convertMillisToTimeFormat(it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)))
//                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                videos.add(Video(id, title, duration, data))
            }
//            it.close()
        }
        videoAdapter.setData(videos)
    }

    private fun initSocket(view: View) {
        socketViewModel = (activity as HomeActivity).socketViewModel
        Log.e("initSocket", "video: ${socketViewModel.videoInRoom}")

        socketResponseJob = lifecycleScope.launch {
            socketViewModel.dataRes.collect { data ->
                Log.e("MainFragment", "Data incoming: $data")
                if (this@MainFragment.isVisible)
                    when (data.channel) {
                        "create-room" -> {
                            socketViewModel.myName = data.user?.name ?: "Me"
                            socketViewModel.nUsers = data.nUsers ?: 0
                            socketViewModel.roomCode = data.roomCode
                            socketViewModel.amICreator = true
                            progressDialog.dismiss()

//                            ((activity as HomeActivity).supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment)
//                                .navController.navigate(R.id.action_mainFragment_to_playerFragment)
                            Navigation.findNavController(view)
                                .navigate(R.id.action_mainFragment_to_playerFragment)
                        }
                        "join-room" -> {
                            progressDialog.dismiss()
                            if (data.err == true) {
                                Snackbar.make(binding.root, data.message, Snackbar.LENGTH_LONG)
                                    .show()
                            } else {
                                socketViewModel.videoInRoom = Video(
                                    0, title = data.message,
                                    duration = data.info,
                                    path = "",
                                    isPlaying = data.isVideoPlaying,
                                    currentTime = data.currentTime
                                )
                                socketViewModel.myName = data.user?.name ?: "Me"
                                socketViewModel.nUsers = data.nUsers ?: 0
                                socketViewModel.roomCode = data.roomCode


                                ((activity as HomeActivity).supportFragmentManager.findFragmentById(
                                    R.id.fragmentContainerView
                                ) as NavHostFragment)
                                    .navController.navigate(R.id.action_mainFragment_to_fileManagerFragment)

//                            Navigation.findNavController(view)
//                                .navigate(R.id.action_mainFragment_to_fileManagerFragment)

                            }
                        }
                        "error" -> {
                            Log.e("SocketViewModel", "$data")
                        }
                        else -> {
                            Log.e("MainFragment", "Invalid channel data=$data")
                        }
                    }
            }
        }

        /*lifecycleScope.launchWhenCreated {
            socketViewModel.dataRes.collect { data ->
                Log.e("MainFragment", "Data incoming: $data")
                if (socketViewModel.myName == "Me" && this@MainFragment.isVisible) {
                    when (data.channel) {
                        "create-room" -> {
                            socketViewModel.myName = data.user?.name ?: "Me"
                            socketViewModel.nUsers = data.nUsers ?: 0
                            socketViewModel.roomCode = data.roomCode
                            socketViewModel.amICreator = true
                            progressDialog.dismiss()

                            ((activity as HomeActivity).supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment)
                                .navController.navigate(R.id.action_mainFragment_to_playerFragment)
//                            Navigation.findNavController(view)
//                                .navigate(R.id.action_mainFragment_to_playerFragment)
                        }
                        "join-room" -> {
                            progressDialog.dismiss()
                            if (data.err == true) {
                                Snackbar.make(binding.root, data.message, Snackbar.LENGTH_LONG)
                                    .show()
                            } else {
                                socketViewModel.videoInRoom = Video(
                                    0, title = data.message,
                                    duration = data.info,
                                    path = "",
                                    isPlaying = data.isVideoPlaying,
                                    currentTime = data.currentTime
                                )
                                socketViewModel.myName = data.user?.name ?: "Me"
                                socketViewModel.nUsers = data.nUsers ?: 0
                                socketViewModel.roomCode = data.roomCode
                                Navigation.findNavController(view)
                                    .navigate(R.id.action_mainFragment_to_fileManagerFragment)
                            }
                        }
                        "error" -> {
                            Log.e("SocketViewModel", "$data")
                        }
                        else -> {
                            Log.e("MainFragment", "Invalid channel data=$data")
                        }
                    }
                }
            }
        }*/
    }

    private fun convertMillisToTimeFormat(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cursor?.close()
        _binding = null
        socketResponseJob?.cancel()
    }
}