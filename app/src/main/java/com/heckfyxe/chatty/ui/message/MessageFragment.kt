package com.heckfyxe.chatty.ui.message

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.util.GoneImageLoader
import com.heckfyxe.chatty.util.loadCircleUserAvatar
import com.sendbird.android.GroupChannel
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.android.synthetic.main.message_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MessageFragment : Fragment() {

    companion object {
        const val ARG_CHANNEL = "com.heckfyxe.chatty.ARG_CHANNEL"
    }

    private val viewModel: MessageViewModel by viewModel()

    private lateinit var adapter: MessagesListAdapter<Message>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isInitialized) {
            val bytes = arguments?.getByteArray(ARG_CHANNEL)
            val channel = GroupChannel.buildFromSerializedData(bytes) as GroupChannel
            viewModel.init(channel)
        }

        adapter = MessagesListAdapter(
            viewModel.userId,
            GoneImageLoader()
        )

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.messagesUpdatedLiveData.observe(this, Observer {
            adapter.clear()
            adapter.addToEnd(viewModel.messageList, false)
        })

        viewModel.interlocutor.observe(this, Observer {
            dialogUserNickname?.text = it.name
            dialogUserAvatar?.loadCircleUserAvatar(it)
        })

        viewModel.errors.observe(this, Observer {
            Log.e("MessageFragment", "error", it)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.message_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageList?.setAdapter(adapter)
        adapter.setLoadMoreListener { page, totalItemsCount ->
            Log.i("MessageFragment", "page: $page, total: $totalItemsCount")
            viewModel.getPrevMessages()
        }

        messageTextInput?.setInputListener {
            viewModel.sendMessage(it.toString()) { }
            return@setInputListener true
        }
    }
}
