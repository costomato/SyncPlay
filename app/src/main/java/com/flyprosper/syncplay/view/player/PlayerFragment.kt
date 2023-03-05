package com.flyprosper.syncplay.view.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.flyprosper.syncplay.BuildConfig
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.adapters.chat.ChatAdapter
import com.flyprosper.syncplay.databinding.FragmentPlayerBinding
import com.flyprosper.syncplay.network.model.MessageData
import com.flyprosper.syncplay.network.model.User
import com.flyprosper.syncplay.view.custom.CustomVideoView
import com.flyprosper.syncplay.view.home.HomeActivity
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var socketViewModel: SocketViewModel

    private var socketResponseJob: Job? = null

    private var currentPosition = 0L
    private var isPlaying = false
    private var mediaController: MediaController? = null

    private var mInterstitialAd: InterstitialAd? = null

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

        init(savedInstanceState)
        initSocket()
    }

    private fun init(savedInstanceState: Bundle?) {
//        load ad on exit
        InterstitialAd.load(
            requireContext(),
            BuildConfig.INT_PLAYER_FRAGMENT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            mInterstitialAd = null
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            mInterstitialAd = null
                        }
                    }
                }
            })


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
        mediaController = MediaController(requireContext())
        binding.videoPlayer.setMediaController(mediaController)
        mediaController?.setAnchorView(binding.videoPlayer)

        if (savedInstanceState != null) {
            socketViewModel.intendVideoAction = false
            Log.e("init", "seeking video to = ${savedInstanceState.getLong("currentPosition")}")
            binding.videoPlayer.seekTo(savedInstanceState.getLong("currentPosition").toInt())
            if (savedInstanceState.getBoolean("isPlaying"))
                binding.videoPlayer.start()
            socketViewModel.intendVideoAction = true
        }

        if (!socketViewModel.amICreator) {
            socketViewModel.intendVideoAction = false
            if (socketViewModel.videoInRoom?.isPlaying == false) {
                binding.videoPlayer.pause()
            } else {
                binding.videoPlayer.seekTo(socketViewModel.videoInRoom?.currentTime?.toInt() ?: 0)
            }
            socketViewModel.intendVideoAction = true
        }

        binding.videoPlayer.setActionListener(object : CustomVideoView.ActionListener {
            override fun onPause() {
                Log.e(
                    "onPause",
                    "video action = ${socketViewModel.intendVideoAction}"
                )
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
                            currentTime = binding.videoPlayer.currentPosition.toLong()
                        )
                    )
                }
            }

            override fun onResume() {
                Log.e("videoPlayer", "onResume")
            }

            override fun onPlay() {
                Log.e(
                    "onPlay",
                    "video action = ${socketViewModel.intendVideoAction}"
                )
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
                            isVideoPlaying = true,
                            currentTime = binding.videoPlayer.currentPosition.toLong()
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
                            currentTime = binding.videoPlayer.currentPosition.toLong()
                        )
                    )
                }
            }

        })
    }

    private fun initSocket() {
        socketResponseJob = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            socketViewModel.dataRes.collectLatest { data ->
                Log.e("PlayerFrag socket", "data incoming = $data")
                when (data.channel) {
                    "join-room" -> {
                        socketViewModel.nUsers = data.nUsers ?: 0
                        binding.tvPeopleWatching?.text =
                            getString(R.string.people_watching, "${socketViewModel.nUsers}")

                        socketViewModel.intendVideoAction = false

                        binding.videoPlayer.pause()

                        if (socketViewModel.amICreator) {
                            Log.e(
                                "PlayerFrag join-room",
                                "Some user joined, sending request to sync video"
                            )
                            socketViewModel.sendData(
                                MessageData(
                                    channel = "media-sync",
                                    User(socketViewModel.myName, true),
                                    roomCode = socketViewModel.roomCode,
                                    message = "seek",
                                    isVideoPlaying = false,
                                    currentTime = binding.videoPlayer.currentPosition.toLong()
                                )
                            )
                        }

                        socketViewModel.intendVideoAction = true
                    }
                    "media-sync" -> {
                        socketViewModel.videoInRoom?.isPlaying = data.isVideoPlaying
                        socketViewModel.videoInRoom?.currentTime = data.currentTime

                        if (data.user?.name != socketViewModel.myName) {
                            socketViewModel.intendVideoAction = false
                            when (data.message) {
                                "pause" -> {
                                    Log.e(
                                        "receivedPause",
                                        "Seeking to ${data.currentTime} and pausing"
                                    )
                                    binding.videoPlayer.seekTo(data.currentTime?.toInt() ?: 0)
                                    binding.videoPlayer.pause()
                                }
                                "start" -> {
                                    Log.e(
                                        "receivedStart",
                                        "Seeking to ${data.currentTime} and playing"
                                    )
                                    binding.videoPlayer.seekTo(data.currentTime?.toInt() ?: 0)
                                    binding.videoPlayer.start()
                                    Log.e(
                                        "receivedStart",
                                        "Video started at pos ${binding.videoPlayer.currentPosition}"
                                    )
                                }
                                "seek" -> {
                                    Log.e("receivedSeek", "Seeking to ${data.currentTime}")
                                    binding.videoPlayer.seekTo(data.currentTime?.toInt() ?: 0)
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
                                socketViewModel.chatAdapter.notifyItemInserted(socketViewModel.chats.size)
                                binding.rvChat!!.smoothScrollToPosition(socketViewModel.chats.size - 1)
                            }
                        }
                    }
                    "exit-room" -> {
                        Log.e("PlayerFrag exit-room", "A user left the room")
                        Snackbar.make(
                            binding.root,
                            "${data.user?.name} left with message ${data.message}",
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        socketViewModel.nUsers = data.nUsers ?: 0
                        binding.tvPeopleWatching?.text =
                            getString(R.string.people_watching, socketViewModel.nUsers.toString())

                        if (socketViewModel.nUsers == 0)
                            ((activity as HomeActivity).supportFragmentManager.findFragmentById(
                                R.id.fragmentContainerView
                            ) as NavHostFragment)
                                .navController.popBackStack()
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
        currentPosition = binding.videoPlayer.currentPosition.toLong()
        isPlaying = binding.videoPlayer.isPlaying == true
    }

    override fun onResume() {
        super.onResume()
        socketViewModel.intendVideoAction = true
        Log.e("onResume", "video action true now")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.e("onSaveInstanceState", "Saving data... Pos = $currentPosition")
        outState.putLong("currentPosition", currentPosition)
        outState.putBoolean("isPlaying", isPlaying)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        Log.e("PlayerFrag onDestroy", "Destroying player fragment")

        if (isRemoving) {
            Log.e("PlayerFrag onDestroy", "Resetting svm vars...")

            socketViewModel.sendData(
                MessageData(
                    channel = "exit-room",
                    roomCode = socketViewModel.roomCode,
                    user = User(name = socketViewModel.myName, isAdmin = true),
                    message = "Goodbye!"
                )
            )

            socketViewModel.myName = "Me"
            socketViewModel.roomCode = null
            socketViewModel.videoInRoom = null
            socketViewModel.nUsers = 0
            socketViewModel.amICreator = false
            socketViewModel.chats.clear()

            mInterstitialAd?.show(activity as HomeActivity)
        }
        socketViewModel.intendVideoAction = false

        mediaController?.removeAllViews()
        mediaController = null

        socketResponseJob?.cancel()
        socketResponseJob = null
    }
}

/*
// previously developed using exoplayer

package com.flyprosper.syncplay.view.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.adapters.chat.ChatAdapter
import com.flyprosper.syncplay.databinding.FragmentPlayerBinding
import com.flyprosper.syncplay.network.model.MessageData
import com.flyprosper.syncplay.network.model.User
import com.flyprosper.syncplay.view.home.HomeActivity
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var socketViewModel: SocketViewModel

    private var socketResponseJob: Job? = null

    private var currentPosition = 0L
    private var isPlaying = false

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

            socketViewModel.player = ExoPlayer.Builder(requireContext()).build()

            socketViewModel.videoInRoom?.path?.let { MediaItem.fromUri(it) }
                ?.let { socketViewModel.player?.setMediaItem(it) }

            socketViewModel.player?.prepare()
            socketViewModel.player?.addListener(object : Player.Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                    Log.e(
                        "onPositionDiscontinuity",
                        "video action = ${socketViewModel.intendVideoAction}"
                    )
                    if (socketViewModel.intendVideoAction && reason == ExoPlayer.DISCONTINUITY_REASON_SEEK) {
                        Log.e(
                            "videoPlay",
                            "Sending seek request to position ${socketViewModel.player?.currentPosition}"
                        )

                        socketViewModel.sendData(
                            MessageData(
                                channel = "media-sync",
                                roomCode = socketViewModel.roomCode,
                                message = "seek",
                                user = User(name = socketViewModel.myName, true),
                                isVideoPlaying = socketViewModel.player?.isPlaying,
                                currentTime = socketViewModel.player?.currentPosition
                            )
                        )
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    Log.e(
                        "onIsPlayingChanged",
                        "video action = ${socketViewModel.intendVideoAction}"
                    )
                    if (socketViewModel.intendVideoAction) {
                        if (isPlaying) {
                            Log.e(
                                "videoPlay",
                                "Sending play request from position ${socketViewModel.player?.currentPosition}"
                            )

                            socketViewModel.sendData(
                                MessageData(
                                    channel = "media-sync",
                                    roomCode = socketViewModel.roomCode,
                                    message = "start",
                                    user = User(name = socketViewModel.myName, true),
                                    isVideoPlaying = true,
                                    currentTime = socketViewModel.player?.currentPosition
                                )
                            )
                        } else {
                            Log.e(
                                "videoPlay",
                                "Sending pause request on pos ${socketViewModel.player?.currentPosition}"
                            )

                            socketViewModel.sendData(
                                MessageData(
                                    channel = "media-sync",
                                    roomCode = socketViewModel.roomCode,
                                    message = "pause",
                                    user = User(name = socketViewModel.myName, true),
                                    isVideoPlaying = false,
                                    currentTime = socketViewModel.player?.currentPosition
                                )
                            )
                        }
                    }
                }
            })

        }

        init(savedInstanceState)
        initSocket()
    }

    private fun init(savedInstanceState: Bundle?) {
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

        binding.videoPlayer.player = socketViewModel.player
        if (savedInstanceState != null) {
            socketViewModel.intendVideoAction = false
            Log.e("init", "seeking video to = ${savedInstanceState.getLong("currentPosition")}")
            socketViewModel.player?.seekTo(savedInstanceState.getLong("currentPosition"))
            if (savedInstanceState.getBoolean("isPlaying"))
                socketViewModel.player?.play()
            socketViewModel.intendVideoAction = true
        }

        if (!socketViewModel.amICreator) {
            socketViewModel.intendVideoAction = false
            if (socketViewModel.videoInRoom?.isPlaying == false) {
                socketViewModel.player?.pause()
            } else {
                socketViewModel.player?.seekTo(socketViewModel.videoInRoom?.currentTime ?: 0L)
            }
            socketViewModel.intendVideoAction = true
        }
    }

    private fun initSocket() {
        socketResponseJob = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            socketViewModel.dataRes.collectLatest { data ->
                Log.e("PlayerFrag socket", "data incoming = $data")
                when (data.channel) {
                    "join-room" -> {
                        socketViewModel.nUsers = data.nUsers ?: 0
                        binding.tvPeopleWatching?.text =
                            getString(R.string.people_watching, "${socketViewModel.nUsers}")

                        socketViewModel.intendVideoAction = false

                        socketViewModel.player?.pause()

                        if (socketViewModel.amICreator) {
                            Log.e(
                                "PlayerFrag join-room",
                                "Some user joined, sending request to sync video"
                            )
                            socketViewModel.sendData(
                                MessageData(
                                    channel = "media-sync",
                                    User(socketViewModel.myName, true),
                                    roomCode = socketViewModel.roomCode,
                                    message = "seek",
                                    isVideoPlaying = false,
                                    currentTime = socketViewModel.player?.currentPosition
                                )
                            )
                        }

                        socketViewModel.intendVideoAction = true
                    }
                    "media-sync" -> {
                        socketViewModel.videoInRoom?.isPlaying = data.isVideoPlaying
                        socketViewModel.videoInRoom?.currentTime = data.currentTime

                        if (data.user?.name != socketViewModel.myName && socketViewModel.player?.playbackState != Player.STATE_ENDED) {
                            socketViewModel.intendVideoAction = false
                            when (data.message) {
                                "pause" -> {
                                    Log.e(
                                        "receivedPause",
                                        "Seeking to ${data.currentTime} and pausing. Command play pause available = ${
                                            socketViewModel.player?.isCommandAvailable(
                                                Player.COMMAND_PLAY_PAUSE
                                            )
                                        }"
                                    )
                                    socketViewModel.player?.seekTo(data.currentTime ?: 0L)
                                    socketViewModel.player?.pause()
                                }
                                "start" -> {
                                    Log.e(
                                        "receivedStart",
                                        "Seeking to ${data.currentTime} and playing"
                                    )
                                    socketViewModel.player?.seekTo(data.currentTime ?: 0L)
                                    socketViewModel.player?.play()
                                    Log.e(
                                        "receivedStart",
                                        "Video started at pos ${socketViewModel.player?.currentPosition}"
                                    )
                                }
                                "seek" -> {
                                    Log.e("receivedSeek", "Seeking to ${data.currentTime}")
                                    socketViewModel.player?.seekTo(data.currentTime ?: 0L)
                                    if (data.isVideoPlaying == true)
                                        socketViewModel.player?.play()
                                    else
                                        socketViewModel.player?.pause()
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
                                socketViewModel.chatAdapter.notifyItemInserted(socketViewModel.chats.size)
                                binding.rvChat!!.smoothScrollToPosition(socketViewModel.chats.size - 1)
                            }
                        }
                    }
                    "exit-room" -> {
                        Log.e("PlayerFrag exit-room", "A user left the room")
                        Snackbar.make(
                            binding.root,
                            "${data.user?.name} left with message ${data.message}",
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        socketViewModel.nUsers = data.nUsers ?: 0
                        binding.tvPeopleWatching?.text =
                            getString(R.string.people_watching, socketViewModel.nUsers.toString())

                        if (socketViewModel.nUsers == 0)
                            ((activity as HomeActivity).supportFragmentManager.findFragmentById(
                                R.id.fragmentContainerView
                            ) as NavHostFragment)
                                .navController.popBackStack()
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
        currentPosition = socketViewModel.player?.currentPosition ?: 0L
        isPlaying = socketViewModel.player?.isPlaying == true
    }

    override fun onResume() {
        super.onResume()
        socketViewModel.intendVideoAction = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.e("onSaveInstanceState", "Saving data... Pos = $currentPosition")
        outState.putLong("currentPosition", currentPosition)
        outState.putBoolean("isPlaying", isPlaying)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        Log.e("PlayerFrag onDestroy", "Destroying player fragment")

        if (isRemoving) {
            Log.e("PlayerFrag onDestroy", "Resetting svm vars...")

            socketViewModel.sendData(
                MessageData(
                    channel = "exit-room",
                    roomCode = socketViewModel.roomCode,
                    user = User(name = socketViewModel.myName, isAdmin = true),
                    message = "Goodbye!"
                )
            )

            socketViewModel.myName = "Me"
            socketViewModel.roomCode = null
            socketViewModel.videoInRoom = null
            socketViewModel.nUsers = 0
            socketViewModel.amICreator = false
            socketViewModel.chats.clear()
            socketViewModel.intendVideoAction = false
            socketViewModel.player?.clearMediaItems()
            socketViewModel.player?.clearVideoSurface()
            socketViewModel.player = null
        }

        socketResponseJob?.cancel()
        socketResponseJob = null
    }
}
*/
