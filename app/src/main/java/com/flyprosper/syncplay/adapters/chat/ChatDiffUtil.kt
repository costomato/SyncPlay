package com.flyprosper.syncplay.adapters.chat

import androidx.recyclerview.widget.DiffUtil
import com.flyprosper.syncplay.network.model.MessageData

class ChatDiffUtil(
    private val oldList: List<MessageData>,
    private val newList: List<MessageData>
) : DiffUtil.Callback() {

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
            oldList[oldItemPosition].channel != newList[newItemPosition].channel -> false
            oldList[oldItemPosition].user?.name != newList[newItemPosition].user?.name -> false
            oldList[oldItemPosition].user?.isAdmin != newList[newItemPosition].user?.isAdmin -> false
            oldList[oldItemPosition].roomCode != newList[newItemPosition].roomCode -> false
            oldList[oldItemPosition].message != newList[newItemPosition].message -> false
            oldList[oldItemPosition].err != newList[newItemPosition].err -> false
            oldList[oldItemPosition].appVersion != newList[newItemPosition].appVersion -> false
            oldList[oldItemPosition].info != newList[newItemPosition].info -> false
            else -> true
        }
    }
}