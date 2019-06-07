package com.heckfyxe.chatty.ui.message

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.util.loadCircleUserAvatar
import com.stfalcon.chatkit.messages.MessageInput
import kotlinx.android.synthetic.main.message_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MessageFragment : Fragment() {

    private val viewModel: MessageViewModel by viewModel { parametersOf(args.channelId) }

    private val args: MessageFragmentArgs by navArgs()

    private lateinit var adapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = MessageAdapter()
        layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, true)

        observeViewModel()
    }

    private fun scrollTo(position: Int) {
        layoutManager.scrollToPosition(position)
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this, Observer {
            adapter.update(it)
        })

        viewModel.needsToScroll.observe(this, Observer {
            scrollTo(it)
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

        val appCompatActivity = activity as? AppCompatActivity
        appCompatActivity?.setSupportActionBar(messageToolbar)
        NavigationUI.setupWithNavController(messageToolbar, findNavController())

        messageList?.layoutManager = layoutManager
        messageList?.adapter = adapter
        adapter.onLoadMoreListener = object : MessageAdapter.OnLoadMoreListener {
            override fun onLoadMore(page: Int, total: Int) {
                viewModel.getPrevMessages()
            }
        }
        messageList?.addOnScrollListener(RecyclerScrollMoreListener(layoutManager, adapter))

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
