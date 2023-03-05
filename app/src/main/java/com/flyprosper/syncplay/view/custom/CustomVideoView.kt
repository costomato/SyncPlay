package com.flyprosper.syncplay.view.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.VideoView

/**
 * This view was removed from this project and ExoPlayer was used instead
 * But then ExoPlayer was removed and this was used
 */
class CustomVideoView(context: Context, attrs: AttributeSet?) : VideoView(context, attrs) {

    private lateinit var listener: ActionListener

    fun setActionListener(listener: ActionListener) {
        this.listener = listener
    }

    override fun pause() {
        super.pause()
        Log.e("CustomVideoView", "pause")
        if (this::listener.isInitialized)
            listener.onPause()
    }

    override fun resume() {
        super.resume()
        Log.e("CustomVideoView", "resume")
        if (this::listener.isInitialized)
            listener.onResume()
    }

    override fun start() {
        super.start()
        Log.e("CustomVideoView", "start")
        if (this::listener.isInitialized)
            listener.onPlay()
    }

    override fun seekTo(msec: Int) {
        super.seekTo(msec)
        Log.e("CustomVideoView", "seek")
        if (this::listener.isInitialized)
            listener.onSeekComplete(msec)
    }

    interface ActionListener {
        fun onPause()
        fun onResume()
        fun onPlay()
        fun onSeekComplete(millis: Int)
    }
}