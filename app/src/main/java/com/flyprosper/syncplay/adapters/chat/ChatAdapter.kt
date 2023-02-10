package com.flyprosper.syncplay.adapters.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flyprosper.syncplay.R
import com.flyprosper.syncplay.databinding.ItemChatBinding
import com.flyprosper.syncplay.network.model.MessageData

class ChatAdapter(
    private val listener: OnItemClickListener,
    private val myName: String
) :
    RecyclerView.Adapter<ChatAdapter.ChatRecyclerViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var oldChatList = emptyList<MessageData>()

    inner class ChatRecyclerViewHolder(val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRecyclerViewHolder {
        return ChatRecyclerViewHolder(
            ItemChatBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ChatRecyclerViewHolder, position: Int) {
        val messageData = oldChatList[position]
        holder.binding.tvBody.text = messageData.message
        val sender = "${messageData.user?.name} said:"
        holder.binding.tvSender.text = sender

        if(messageData.user?.name == myName) {
            val params = holder.binding.msgCard.layoutParams as ConstraintLayout.LayoutParams
            params.endToEnd = holder.binding.msgParent.id
            params.topToTop = holder.binding.msgParent.id
            params.bottomToBottom =  holder.binding.msgParent.id
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            holder.binding.msgContainer.setBackgroundResource(R.color.msg_sent)
            holder.binding.msgCard.requestLayout()
        } else {
            val params = holder.binding.msgCard.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = holder.binding.msgParent.id
            params.topToTop = holder.binding.msgParent.id
            params.bottomToBottom = holder.binding.msgParent.id
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            holder.binding.msgContainer.setBackgroundResource(R.color.msg_received)
            holder.binding.msgCard.requestLayout()
        }

        holder.binding.root.setOnClickListener {
            listener.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return oldChatList.size
    }

    fun setData(newChatList: List<MessageData>) {
        val diffUtil = ChatDiffUtil(oldChatList, newChatList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        oldChatList = newChatList
        diffResult.dispatchUpdatesTo(this)
    }

}