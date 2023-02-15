package com.flyprosper.syncplay.view.files

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flyprosper.syncplay.BuildConfig
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.adapters.files.FileManagerAdapter
import com.flyprosper.syncplay.databinding.FragmentFileManagerBinding
import com.flyprosper.syncplay.dialogs.CustomAlertDialog
import com.flyprosper.syncplay.dialogs.ProgressDialog
import com.flyprosper.syncplay.model.Video
import com.flyprosper.syncplay.model.VideoDirectory
import com.flyprosper.syncplay.network.model.MessageData
import com.flyprosper.syncplay.utils.VideoFileManager
import com.flyprosper.syncplay.view.home.HomeActivity
import com.flyprosper.syncplay.viewmodel.SocketViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class FileManagerFragment : Fragment() {

    private var _binding: FragmentFileManagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var socketViewModel: SocketViewModel
    private lateinit var progressDialog: ProgressDialog

    private lateinit var videoFileManager: VideoFileManager
    private lateinit var fileManagerAdapter: FileManagerAdapter

    private lateinit var videoDirs: List<VideoDirectory>

    private var socketResponseJob: Job? = null

    // Not really required in this case as i am not going to deal with subdirectories.
    // Just using the stack rule as a practice
    private val dirStack: Stack<String> = Stack()

    private lateinit var rvFileMgr: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentFileManagerBinding.inflate(inflater, container, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Do work here
        if (savedInstanceState == null) {
            init(view)
            initSocket(view)
        }
    }

    private fun init(view: View) {
        progressDialog = ProgressDialog(requireContext())

        rvFileMgr = binding.rvFileMgr
        rvFileMgr.layoutManager = object : GridLayoutManager(requireContext(), 2) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                lp.height = (width / 2 * .6).toInt()
                return true
            }
        }
        videoFileManager = VideoFileManager()
        fileManagerAdapter = FileManagerAdapter(object : FileManagerAdapter.OnFileClickListener {
            override fun onFileClick(file: VideoDirectory) {
                if (file.isDirectory) {
                    progressDialog.show()
                    fileManagerAdapter.setData(videoFileManager.getVideoFiles(file.path))
                    dirStack.push(file.path)
                    progressDialog.dismiss()
                } else {
                    progressDialog.show()

                    val duration = file.path.getVideoDuration()
                    if (socketViewModel.videoInRoom == null) {
                        if (socketViewModel.socketResult) {
                            val messageData = MessageData(
                                channel = "create-room",
                                message = file.name,
                                appVersion = BuildConfig.VERSION_NAME,
                                info = duration
                            )
                            socketViewModel.videoInRoom = Video(
                                0, file.name,
                                duration, file.path
                            )
                            socketViewModel.sendData(messageData)
                        } else
                            Snackbar.make(
                                binding.root,
                                getString(R.string.please_wait_socket),
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                    } else {
                        progressDialog.dismiss()

                        if (duration != socketViewModel.videoInRoom?.duration || file.name != socketViewModel.videoInRoom?.title) {
                            val customAlertDialog = CustomAlertDialog()
                            val builder = customAlertDialog.builder(
                                "Sure you wanna continue?",
                                "The duration or title of selected video does not match the one in the room. Continue?",
                                requireContext(),
                                object : CustomAlertDialog.OnChoiceClickListener {
                                    override fun onPositiveClick() {
                                        socketViewModel.videoInRoom =
                                            Video(0, file.name, duration, file.path)
                                        Navigation.findNavController(view)
                                            .navigate(R.id.action_fileManagerFragment_to_playerFragment)
                                    }
                                })
                            customAlertDialog.createDialog(builder)
                                .show()
                        } else {
                            socketViewModel.videoInRoom = Video(0, file.name, duration, file.path)
                            Navigation.findNavController(view)
                                .navigate(R.id.action_fileManagerFragment_to_playerFragment)
                        }
                    }
                }
            }
        })

        context?.contentResolver?.let {
            videoFileManager.getVideoDirectories(
                it
            )
        }?.let { videoDirs = it }

        fileManagerAdapter.setData(videoDirs)
        rvFileMgr.adapter = fileManagerAdapter


//        handle back button click
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && dirStack.isNotEmpty()) {
//                val previousPath = dirStack.pop()
//                fileManagerAdapter.setData(videoFileManager.getVideoFiles(previousPath))
                dirStack.pop()
                fileManagerAdapter.setData(videoDirs)
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun initSocket(view: View) {
        socketViewModel = (activity as HomeActivity).socketViewModel
        Log.e("FmFragment", "svm video: ${socketViewModel.videoInRoom}")

        if (socketViewModel.videoInRoom == null) {
            socketResponseJob = viewLifecycleOwner.lifecycleScope.launch {
                socketViewModel.dataRes.collect { data ->
                    Log.e("FmFragment", "Received data: $data")
                    when (data.channel) {
                        "create-room" -> {
                            progressDialog.dismiss()

                            if (data.err == true) {
                                Snackbar.make(binding.root, data.message, Snackbar.LENGTH_LONG)
                                    .show()
                            } else {
                                socketViewModel.myName = data.user?.name ?: "Me"
                                socketViewModel.nUsers = data.nUsers ?: 0
                                socketViewModel.roomCode = data.roomCode
                                socketViewModel.amICreator = true
                                Navigation.findNavController(view)
                                    .navigate(R.id.action_fileManagerFragment_to_playerFragment)
                            }
                        }
                        "error" -> {
                            Log.e("FileManagerFrag", "$data")
                        }
                        else -> {
                            Log.e("FileManagerFrag", "Invalid channel data=$data")
                        }

                    }
                }
            }
        } else {
            binding.tvStorage.text =
                getString(R.string.video_in_room, "${socketViewModel.videoInRoom?.title}")
        }
    }

    private fun String.getVideoDuration(): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(requireContext(), Uri.parse(this))
        val duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        retriever.release()

        val hours = TimeUnit.MILLISECONDS.toHours(duration) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        socketResponseJob?.cancel()
    }
}