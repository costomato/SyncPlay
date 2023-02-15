package com.flyprosper.syncplay.view.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.adapters.chat.ChatAdapter
import com.flyprosper.syncplay.databinding.FragmentPlayerBinding
import com.flyprosper.syncplay.model.Video
import com.flyprosper.syncplay.network.model.MessageData
import com.flyprosper.syncplay.network.model.User
import com.flyprosper.syncplay.view.custom.CustomVideoView
import com.flyprosper.syncplay.view.home.HomeActivity
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var socketViewModel: SocketViewModel

    private var socketResponseJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("onViewCreated", "Player frag created")
//        Do work here
        socketViewModel = (activity as HomeActivity).socketViewModel

        if (savedInstanceState == null) {
            socketViewModel.chatAdapter = ChatAdapter(object : ChatAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
//                    will see this area in future
                }
            }, socketViewModel.myName)
            socketViewModel.chatAdapter.setData(socketViewModel.chats)
        }

        init()
        initSocket()
    }

    private fun init() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            recyclerview
            binding.rvChat!!.adapter = socketViewModel.chatAdapter

//            setting the text views
            binding.tvMovieTitle?.text = socketViewModel.videoInRoom?.title
            binding.tvInviteLink?.text = socketViewModel.roomCode
            binding.tvPeopleWatching?.text =
                getString(R.string.people_watching, "${socketViewModel.nUsers}")

            binding.tvInviteLink?.setOnClickListener {
                (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText("Room code", socketViewModel.roomCode)
                )
                Snackbar.make(
                    binding.root,
                    "Room code: ${socketViewModel.roomCode} copied to clipboard",
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }

            binding.tvInviteButton?.setOnClickListener {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(
                                Intent.EXTRA_TEXT,
                                "Join me on SyncPlay. Use my room code:\n\n${socketViewModel.roomCode}"
                            )
                            .putExtra(
                                Intent.EXTRA_TITLE,
                                "Share room code"
                            ),
                        "Share room code"
                    )
                )
            }

            binding.ivSendMsg?.setOnClickListener {
                val msg = binding.etMessage?.text?.trim().toString()
                if (msg.isNotEmpty()) {
                    binding.etMessage?.setText("")
                    val messageData = MessageData(
                        channel = "chat",
                        user = User(
                            name = socketViewModel.myName,
                            isAdmin = true
                        ),
                        roomCode = socketViewModel.roomCode,
                        message = msg
                    )
                    socketViewModel.sendData(
                        messageData
                    )
                    socketViewModel.chats.push(messageData)
                    socketViewModel.chatAdapter.notifyItemInserted(socketViewModel.chats.size)
                    binding.rvChat!!.smoothScrollToPosition(socketViewModel.chats.size - 1)
                }
            }
        }

        binding.videoPlayer.setVideoPath(socketViewModel.videoInRoom?.path)
        val mediaController = MediaController(requireContext())
        binding.videoPlayer.setMediaController(mediaController)
        mediaController.setAnchorView(binding.videoPlayer)
        mediaController.setMediaPlayer(binding.videoPlayer)

        binding.videoPlayer.setActionListener(object : CustomVideoView.ActionListener {
            override fun onPause() {
                if (socketViewModel.intendVideoAction) {
                    Log.e(
                        "videoPlay",
                        "Sending pause request on pos ${binding.videoPlayer.currentPosition}"
                    )
                    socketViewModel.sendData(
                        MessageData(
                            channel = "media-sync",
                            roomCode = socketViewModel.roomCode,
                            message = "pause",
                            user = User(name = socketViewModel.myName, true),
                            isVideoPlaying = false,
                            currentTime = binding.videoPlayer.currentPosition
                        )
                    )
                }
            }

            override fun onResume() {
                if (socketViewModel.intendVideoAction) {
                    Log.e("videoPlay", "Sending resume request")
                    socketViewModel.sendData(
                        MessageData(
                            channel = "media-sync",
                            roomCode = socketViewModel.roomCode,
                            message = "resume",
                            user = User(name = socketViewModel.myName, true),
                            isVideoPlaying = true,
                            currentTime = binding.videoPlayer.currentPosition
                        )
                    )
                }
            }

            override fun onPlay() {
                if (socketViewModel.intendVideoAction) {
                    Log.e(
                        "videoPlay",
                        "Sending play request from position ${binding.videoPlayer.currentPosition}"
                    )
                    socketViewModel.sendData(
                        MessageData(
                            channel = "media-sync",
                            roomCode = socketViewModel.roomCode,
                            message = "start",
                            user = User(name = socketViewModel.myName, true),
                            isVideoPlaying = binding.videoPlayer.isPlaying,
                            currentTime = binding.videoPlayer.currentPosition
                        )
                    )
                }
            }

            override fun onSeekComplete(millis: Int) {
                if (socketViewModel.intendVideoAction) {
                    Log.e(
                        "videoPlay",
                        "Sending seek request to position ${binding.videoPlayer.currentPosition}"
                    )
                    socketViewModel.sendData(
                        MessageData(
                            channel = "media-sync",
                            roomCode = socketViewModel.roomCode,
                            message = "seek",
                            user = User(name = socketViewModel.myName, true),
                            isVideoPlaying = binding.videoPlayer.isPlaying,
                            currentTime = binding.videoPlayer.currentPosition
                        )
                    )
                }
            }
        })

        if (!socketViewModel.amICreator) {
            if (socketViewModel.videoInRoom?.isPlaying == false) {
                binding.videoPlayer.pause()
            } else {
                binding.videoPlayer.seekTo(socketViewModel.videoInRoom?.currentTime ?: 0)
                binding.videoPlayer.start()
            }
            socketViewModel.intendVideoAction = true
        }


//        handle back button click
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                Log.e("setOnKeyListener", "Resetting svm vars...")
                socketViewModel.myName = "Me"
                socketViewModel.roomCode = null
                socketViewModel.videoInRoom = null
                socketViewModel.nUsers = 0
                socketViewModel.amICreator = false
            }
            false
        }

    }

    private fun initSocket() {
        socketResponseJob = viewLifecycleOwner.lifecycleScope.launch {
            socketViewModel.dataRes.collect { data ->
                when (data.channel) {
                    "join-room" -> {
                        socketViewModel.nUsers = data.nUsers ?: 0
                        binding.tvPeopleWatching?.text =
                            getString(R.string.people_watching, "${socketViewModel.nUsers}")

                        socketViewModel.intendVideoAction = false

                        binding.videoPlayer.pause()
                        socketViewModel.sendData(
                            MessageData(
                                channel = "media-sync",
                                User(socketViewModel.myName, true),
                                roomCode = socketViewModel.roomCode,
                                message = "",
                                isVideoPlaying = false,
                                currentTime = binding.videoPlayer.currentPosition
                            )
                        )

                        socketViewModel.intendVideoAction = true
                    }
                    "media-sync" -> {
                        socketViewModel.videoInRoom = Video(
                            0,
                            socketViewModel.videoInRoom?.title ?: "",
                            socketViewModel.videoInRoom?.duration,
                            socketViewModel.videoInRoom?.path ?: "",
                            data.isVideoPlaying,
                            data.currentTime
                        )
                        if (data.user?.name != socketViewModel.myName) {
                            socketViewModel.intendVideoAction = false
                            when (data.message) {
                                "pause" -> {
                                    binding.videoPlayer.seekTo(data.currentTime ?: 0)
                                    binding.videoPlayer.pause()
                                }
                                "resume" -> {
                                    binding.videoPlayer.seekTo(data.currentTime ?: 0)
                                    binding.videoPlayer.resume()
                                }
                                "start" -> {
                                    Log.e(
                                        "receivedStart",
                                        "Seeking to ${data.currentTime} and playing"
                                    )
                                    binding.videoPlayer.seekTo(data.currentTime ?: 0)
                                    binding.videoPlayer.start()
                                    Log.e(
                                        "receivedStart",
                                        "Video started at pos ${binding.videoPlayer.currentPosition}"
                                    )
                                }
                                "seek" -> {
                                    Log.e("receivedSeek", "Seeking to ${data.currentTime}")
                                    binding.videoPlayer.seekTo(data.currentTime ?: 0)
                                    if (data.isVideoPlaying == true)
                                        binding.videoPlayer.start()
                                    else
                                        binding.videoPlayer.pause()
                                }
                            }
                            socketViewModel.intendVideoAction = true
                        }
                    }
                    "chat" -> {
                        if (data.err == true) {
                            Log.e("receivedChatErr", "$data")
                            Toast.makeText(requireContext(), data.message, Toast.LENGTH_LONG)
                                .show()
                        } else {
                            if (data.user?.name != socketViewModel.myName) {
                                socketViewModel.chats.push(data)
//                                socketViewModel.chats.setData(chats)
                                socketViewModel.chatAdapter.notifyItemInserted(socketViewModel.chats.size)
                                binding.rvChat!!.smoothScrollToPosition(socketViewModel.chats.size - 1)
                            }
                        }
                    }
                    "error" -> {
                        Log.e("SocketViewModel", "$data")
                    }
                    else -> {
                        Log.e("PlayerFragment", "Invalid channel data=$data")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        socketViewModel.intendVideoAction = false
        socketViewModel.videoInRoom?.currentTime = binding.videoPlayer.currentPosition
        binding.videoPlayer.pause()
        socketViewModel.intendVideoAction = true
    }

    override fun onResume() {
        super.onResume()
        socketViewModel.intendVideoAction = false
        socketViewModel.videoInRoom?.currentTime?.let { binding.videoPlayer.seekTo(it) }
        socketViewModel.intendVideoAction = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        socketResponseJob?.cancel()
    }
}