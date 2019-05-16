package com.heckfyxe.chatty.ui.message

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.ChatMessage
import com.heckfyxe.chatty.util.GoneImageLoader
import com.heckfyxe.chatty.util.loadCircleUserAvatar
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.android.synthetic.main.message_fragment.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MessageFragment : Fragment() {

    private val userId: String by inject(KOIN_USER_ID)

    private val viewModel: MessageViewModel by viewModel { parametersOf(args.channelId) }

    private val args: MessageFragmentArgs by navArgs()

    private lateinit var adapter: MessagesListAdapter<ChatMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = MessagesListAdapter(
            userId,
            GoneImageLoader()
        )

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this, Observer {
            adapter.clear()
            adapter.addToEnd(it, false)
        })

        viewModel.interlocutorLiveData.observe(this, Observer {
            dialogUserNickname?.text = it.name
            dialogUserAvatar?.loadCircleUserAvatar(it)
        })

        viewModel.interlocutorEmotions.observe(this, Observer {
            interlocutorEmotion?.text = it
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
        adapter.setLoadMoreListener { _, _ ->
            viewModel.getPrevMessages()
        }

        messageTextInput?.setInputListener {
            viewModel.sendTextMessage(it.toString())
            return@setInputListener true
        }

        messageTextInput?.setTypingListener(object : MessageInput.TypingListener {
            override fun onStartTyping() {
                viewModel.startTyping()
            }

            override fun onStopTyping() {
                viewModel.endTyping()
            }
        })
    }
}
