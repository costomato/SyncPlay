package com.flyprosper.syncplay.adapters.files

import androidx.recyclerview.widget.DiffUtil
import com.flyprosper.syncplay.model.VideoDirectory

class FileManagerDiffUtil(private val oldList: List<VideoDirectory>, private val newList: List<VideoDirectory>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return when {
            oldList[oldItemPosition] == newList[newItemPosition] -> true
            else -> false
        }
    }
}