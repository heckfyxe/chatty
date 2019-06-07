package com.heckfyxe.chatty.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.databinding.ItemInMessageBinding
import com.heckfyxe.chatty.databinding.ItemOutMessageBinding
import com.heckfyxe.chatty.room.Message

class MessageAdapter : RecyclerView.Adapter<MessageViewHolder>(), RecyclerScrollMoreListener.OnLoadMoreListener {

    private val messages = mutableListOf<Message>()

    var onLoadMoreListener: OnLoadMoreListener? = null

    companion object {
        private const val TYPE_MESSAGE_IN = 0
        private const val TYPE_MESSAGE_OUT = 1
    }

    private class MessageAdapterDiff(
        private val oldMessages: List<Message>,
        private val newMessages: List<Message>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldMessages.size
        override fun getNewListSize(): Int = newMessages.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldMessages[oldItemPosition].id == newMessages[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldMessages[oldItemPosition] == newMessages[newItemPosition]
    }

    fun update(newMessages: List<Message>) {
        val callback = MessageAdapterDiff(messages, newMessages)
        val diff = DiffUtil.calculateDiff(callback, true)
        messages.clear()
        messages.addAll(newMessages)
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.out)
            TYPE_MESSAGE_OUT
        else
            TYPE_MESSAGE_IN
    }

    private fun createView(parent: ViewGroup, @LayoutRes layout: Int) =
        LayoutInflater.from(parent.context).inflate(layout, parent, false)

    @LayoutRes
    private fun getLayoutByViewType(viewType: Int): Int =
        when (viewType) {
            TYPE_MESSAGE_IN -> R.layout.item_in_message
            TYPE_MESSAGE_OUT -> R.layout.item_out_message
            else -> throw Exception("Unknown viewType")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = createView(parent, getLayoutByViewType(viewType))
        return when (viewType) {
            TYPE_MESSAGE_IN -> MessageInViewHolder(view)
            TYPE_MESSAGE_OUT -> MessageOutViewHolder(view)
            else -> throw Exception("Unknown viewType")
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override val messagesCount: Int
        get() = messages.count()

    override fun onLoadMore(page: Int, total: Int) {
        onLoadMoreListener?.onLoadMore(page, total)
    }

    interface OnLoadMoreListener {
        fun onLoadMore(page: Int, total: Int)
    }
}

abstract class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(message: Message)
}

class MessageInViewHolder(view: View) : MessageViewHolder(view) {
    private val binding: ItemInMessageBinding? = DataBindingUtil.bind(view)

    override fun bind(message: Message) {
        binding?.message = message
        binding?.invalidateAll()
    }
}

class MessageOutViewHolder(view: View) : MessageViewHolder(view) {
    private val binding: ItemOutMessageBinding? = DataBindingUtil.bind(view)

    override fun bind(message: Message) {
        binding?.message = message
        binding?.invalidateAll()
    }
}