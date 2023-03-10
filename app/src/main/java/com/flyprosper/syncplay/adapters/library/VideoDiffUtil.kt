package com.flyprosper.syncplay.adapters.library

import androidx.recyclerview.widget.DiffUtil
import com.flyprosper.syncplay.model.Video

class VideoDiffUtil(private val oldList: List<Video>, private val newList: List<Video>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return when {
            oldList[oldItemPosition].id != newList[newItemPosition].id -> false
            oldList[oldItemPosition].title != newList[newItemPosition].title -> false
            oldList[oldItemPosition].path != newList[newItemPosition].path -> false
            oldList[oldItemPosition].duration != newList[newItemPosition].duration -> false
            else -> true
        }
    }
}