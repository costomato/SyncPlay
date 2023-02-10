package com.flyprosper.syncplay.adapters.files

import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.databinding.ItemRecyclerFileMgrBinding
import com.flyprosper.syncplay.model.VideoDirectory

class FileManagerAdapter(private val listener: OnFileClickListener) :
    RecyclerView.Adapter<FileManagerAdapter.FileManagerViewHolder>() {

    private var oldFilesList = emptyList<VideoDirectory>()

    interface OnFileClickListener {
        fun onFileClick(file: VideoDirectory)
    }

    inner class FileManagerViewHolder(val binding: ItemRecyclerFileMgrBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileManagerViewHolder {
        return FileManagerViewHolder(
            ItemRecyclerFileMgrBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FileManagerViewHolder, position: Int) {
        val file = oldFilesList[position]
        if (file.isDirectory) {
            holder.binding.tvFolderName.text = file.name
            holder.binding.tvFolderName.visibility = VISIBLE
            holder.binding.tvMovieTitle.visibility = INVISIBLE
        } else {
            holder.binding.tvMovieTitle.text = file.name
            holder.binding.tvMovieTitle.visibility = VISIBLE
            holder.binding.tvFolderName.visibility = INVISIBLE
        }
        Glide.with(holder.itemView.context)
            .load(oldFilesList[position].thumbnailPath ?: oldFilesList[position].path)
            .placeholder(R.drawable.ic_round_play_circle_outline_24)
            .error(R.drawable.ic_round_play_circle_outline_24)
            .into(holder.binding.thumbnail)
        holder.binding.root.setOnClickListener {
            listener.onFileClick(file)
        }
    }

    override fun getItemCount(): Int {
        return oldFilesList.size
    }

    fun setData(newFilesList: List<VideoDirectory>) {
        val diffUtil = FileManagerDiffUtil(oldFilesList, newFilesList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        oldFilesList = newFilesList
        diffResult.dispatchUpdatesTo(this)
    }
}
