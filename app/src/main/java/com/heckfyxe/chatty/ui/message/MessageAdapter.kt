package com.heckfyxe.chatty.ui.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.heckfyxe.chatty.databinding.ItemInImageMessageBinding
import com.heckfyxe.chatty.databinding.ItemInMessageBinding
import com.heckfyxe.chatty.databinding.ItemOutImageMessageBinding
import com.heckfyxe.chatty.databinding.ItemOutMessageBinding
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.model.MessageType
import kotlin.math.max
import kotlin.math.min

private const val TYPE_TEXT_MESSAGE_IN = 0
private const val TYPE_TEXT_MESSAGE_OUT = 1
private const val TYPE_IMAGE_MESSAGE_IN = 3
private const val TYPE_IMAGE_MESSAGE_OUT = 4

fun <T : Any> SortedList<T>.withUpdate(block: SortedList<T>.() -> Unit) {
    beginBatchedUpdates()
    block()
    endBatchedUpdates()
}

class MessageAdapter : RecyclerView.Adapter<MessageViewHolder>() {

    interface LoadingListener {
        fun prefetchSize(): Int
        fun loadPreviousMessages(time: Long)
        fun loadNextMessages(time: Long)
    }

    private val callback = object : SortedListAdapterCallback<Message>(this) {
        override fun areItemsTheSame(item1: Message, item2: Message): Boolean =
            if (item1.id == 0L || item2.id == 0L) {
                item1.requestId == item2.requestId
            } else item1.id == item2.id

        override fun compare(o1: Message, o2: Message): Int =
            -o1.time.compareTo(o2.time)

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem == newItem

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onInserted(position: Int, count: Int) {
            notifyItemInserted(position)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRemoved(position)
        }

        override fun onChanged(position: Int, count: Int) {
            notifyItemChanged(position)
        }
    }
    private val messages = SortedList<Message>(Message::class.java, callback)

    private var firstMessageTime = Long.MAX_VALUE
    private var lastMessageTime = 0L

    private var loadingListener: LoadingListener? = null

    fun addMessages(messagesList: List<Message>) {
        messages.withUpdate {
            addAll(messagesList)
        }
        firstMessageTime =
            min(firstMessageTime, messagesList.minBy { it.time }?.time ?: Long.MAX_VALUE)
        lastMessageTime = max(lastMessageTime, messagesList.maxBy { it.time }?.time ?: 0)
    }

    fun messageSent(sentMessage: Message) {
        for (i in 0 until messages.size()) {
            if (messages[i].requestId == sentMessage.requestId) {
                messages.withUpdate {
                    messages.removeItemAt(i)
                    messages.add(sentMessage)
                }
                firstMessageTime = min(firstMessageTime, sentMessage.time)
                lastMessageTime = max(lastMessageTime, sentMessage.time)
                break
            }
        }
    }

    fun setLoadingListener(listener: LoadingListener) {
        loadingListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.out)
            if (message.type == MessageType.TEXT)
                TYPE_TEXT_MESSAGE_OUT
            else
                TYPE_IMAGE_MESSAGE_OUT
        else
            if (message.type == MessageType.TEXT)
                TYPE_TEXT_MESSAGE_IN
            else
                TYPE_IMAGE_MESSAGE_IN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TEXT_MESSAGE_IN -> {
                val binding = ItemInMessageBinding.inflate(inflater, parent, false)
                MessageInViewHolder(binding)
            }
            TYPE_TEXT_MESSAGE_OUT -> {
                val binding = ItemOutMessageBinding.inflate(inflater, parent, false)
                MessageOutViewHolder(binding)
            }
            TYPE_IMAGE_MESSAGE_IN -> {
                val binding = ItemInImageMessageBinding.inflate(inflater, parent, false)
                MessageInImageViewHolder(binding)
            }
            TYPE_IMAGE_MESSAGE_OUT -> {
                val binding = ItemOutImageMessageBinding.inflate(inflater, parent, false)
                MessageOutImageViewHolder(binding)
            }
            else -> throw Exception("Unknown viewType")
        }
    }

    private var previousSize = 0
    private var nextSize = 0

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)

        handleMessageLoading(position)
    }

    private fun handleMessageLoading(position: Int) {
        loadingListener ?: return
        if (messages.size() - position < loadingListener!!.prefetchSize() && previousSize != messages.size()) {
            previousSize = messages.size()
            loadingListener!!.loadPreviousMessages(firstMessageTime)
        }
        if (position < loadingListener!!.prefetchSize() && nextSize != messages.size()) {
            nextSize = messages.size()
            loadingListener!!.loadNextMessages(lastMessageTime)
        }
    }

    override fun getItemCount(): Int = messages.size()
}

abstract class MessageViewHolder(binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(message: Message)
}

class MessageInViewHolder(private val binding: ItemInMessageBinding) : MessageViewHolder(binding) {
    override fun bind(message: Message) {
        binding.message = message
        binding.executePendingBindings()
    }
}

class MessageOutViewHolder(private val binding: ItemOutMessageBinding) :
    MessageViewHolder(binding) {
    override fun bind(message: Message) {
        binding.message = message
        binding.executePendingBindings()
    }
}

class MessageInImageViewHolder(private val binding: ItemInImageMessageBinding) :
    MessageViewHolder(binding) {
    override fun bind(message: Message) {
        binding.message = message
        binding.executePendingBindings()
    }
}

class MessageOutImageViewHolder(private val binding: ItemOutImageMessageBinding) :
    MessageViewHolder(binding) {
    override fun bind(message: Message) {
        binding.message = message
        binding.executePendingBindings()
    }
}