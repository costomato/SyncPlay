package com.flyprosper.syncplay.adapters.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.databinding.ItemRecyclerVideosBinding
import com.flyprosper.syncplay.model.Video
import com.flyprosper.syncplay.model.VideoDirectory


class VideoAdapter(private val listener: OnVideoClickListener) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var oldVideoList = emptyList<Video>()

    interface OnVideoClickListener {
        fun onVideoClick(videoFile: Video)
    }

    inner class VideoViewHolder(val binding: ItemRecyclerVideosBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            ItemRecyclerVideosBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = oldVideoList[position]
        Glide.with(holder.itemView.context).load(video.path)
            .placeholder(R.drawable.ic_round_play_circle_outline_24)
            .error(R.drawable.ic_round_play_circle_outline_24)
            .into(holder.binding.thumbnail)
        holder.binding.duration.text = video.duration
        holder.binding.root.setOnClickListener {
            listener.onVideoClick(video)
        }
    }

    override fun getItemCount(): Int {
        return oldVideoList.size
    }

    fun setData(newVideoList: List<Video>) {
        val diffUtil = VideoDiffUtil(oldVideoList, newVideoList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        oldVideoList = newVideoList
        diffResult.dispatchUpdatesTo(this)
    }
}
