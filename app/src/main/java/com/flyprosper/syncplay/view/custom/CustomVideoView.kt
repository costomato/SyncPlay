package com.flyprosper.syncplay.view.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView


class CustomVideoView(context: Context, attrs: AttributeSet?) : VideoView(context, attrs) {

    private lateinit var listener: ActionListener

    fun setActionListener(listener: ActionListener) {
        this.listener = listener
    }

    override fun pause() {
        super.pause()
        if (this::listener.isInitialized)
            listener.onPause()
    }

    override fun resume() {
        super.resume()
        if (this::listener.isInitialized)
            listener.onResume()
    }

    override fun start() {
        super.start()
        if (this::listener.isInitialized)
            listener.onPlay()
    }

    override fun seekTo(msec: Int) {
        super.seekTo(msec)
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