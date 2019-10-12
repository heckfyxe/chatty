package com.heckfyxe.chatty.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.heckfyxe.chatty.util.room.toChatUser
import com.stfalcon.chatkit.messages.MessageInput
import kotlinx.android.synthetic.main.message_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MessageFragment : Fragment() {

    private val viewModel: MessageViewModel by viewModel {
        parametersOf(
            args.channelId,
            args.user.id
        )
    }

    private val args: MessageFragmentArgs by navArgs()

    private lateinit var adapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = MessageAdapter()
        layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, true)

        observeViewModel()
    }

    private fun scrollDown() {
        layoutManager.scrollToPosition(0)
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this, Observer {
            adapter.submitList(it)
        })

        viewModel.interlocutorEmotions.observe(this, Observer {
            interlocutorEmotion?.text = it
        })

        viewModel.errors.observe(this, Observer { exception ->
            exception?.let {
                viewModel.errors.postValue(null)
                Toast.makeText(context!!, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.scrollDownEvent.observe(this, Observer {
            if (it == false) return@Observer
            scrollDown()
            viewModel.onScrolledDown()
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

        args.user.let {
            dialogUserNickname?.text = it.name
            dialogUserAvatar?.loadCircleUserAvatar(it.toChatUser())
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
