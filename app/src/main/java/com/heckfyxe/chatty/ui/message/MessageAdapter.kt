package com.heckfyxe.chatty.ui.message

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.databinding.ItemInMessageBinding
import com.heckfyxe.chatty.databinding.ItemOutMessageBinding
import com.heckfyxe.chatty.room.RoomMessage
import org.koin.core.KoinComponent
import org.koin.core.inject

class MessageAdapter : PagedListAdapter<RoomMessage, MessageViewHolder>(DIFF), KoinComponent {

    companion object {
        private const val TYPE_MESSAGE_IN = 0
        private const val TYPE_MESSAGE_OUT = 1

        private val DIFF = MessageAdapterDiff()
    }

    private val context: Context by inject()
    private val inflater = LayoutInflater.from(context)

    private class MessageAdapterDiff : DiffUtil.ItemCallback<RoomMessage>() {
        override fun areItemsTheSame(oldItem: RoomMessage, newItem: RoomMessage): Boolean =
            oldItem.time == newItem.time

        override fun areContentsTheSame(oldItem: RoomMessage, newItem: RoomMessage): Boolean =
            oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)!!
        return if (message.out)
            TYPE_MESSAGE_OUT
        else
            TYPE_MESSAGE_IN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when (viewType) {
            TYPE_MESSAGE_IN -> {
                val binding = ItemInMessageBinding.inflate(inflater, parent, false)
                MessageInViewHolder(binding)
            }
            TYPE_MESSAGE_OUT -> {
                val binding = ItemOutMessageBinding.inflate(inflater, parent, false)
                MessageOutViewHolder(binding)
            }
            else -> throw Exception("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }
}

abstract class MessageViewHolder(binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(message: RoomMessage)
}

class MessageInViewHolder(private val binding: ItemInMessageBinding) : MessageViewHolder(binding) {
    override fun bind(message: RoomMessage) {
        binding.message = message
    }
}

class MessageOutViewHolder(private val binding: ItemOutMessageBinding) :
    MessageViewHolder(binding) {
    override fun bind(message: RoomMessage) {
        binding.message = message
    }
}