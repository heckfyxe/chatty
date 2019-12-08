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
import com.heckfyxe.chatty.databinding.MessageFragmentBinding
import com.stfalcon.chatkit.messages.MessageInput
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MessageFragment : Fragment() {

    private val viewModel: MessageViewModel by viewModel {
        parametersOf(
            args.channelId,
            args.user?.id,
            args.lastMessageTime
        )
    }

    private val args: MessageFragmentArgs by navArgs()

    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.scrollDown.observe(this, Observer { needsScrollDown ->
            if (needsScrollDown) {
                layoutManager.scrollToPosition(0)
                viewModel.onScrolledDown()
            }
        })

        viewModel.errors.observe(this, Observer { exception ->
            exception?.let {
                viewModel.onErrorMessagesDisplayed()
                Toast.makeText(context!!, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = MessageFragmentBinding.inflate(inflater).run {
        lifecycleOwner = this@MessageFragment
        interlocutor = args.user
        messageViewModel = viewModel
        executePendingBindings()

        layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, true)
        messageList.layoutManager = layoutManager
        messageList.adapter = viewModel.adapter
        messageTextInput.apply {
            setInputListener {
                viewModel.sendTextMessage(it.toString())
                true
            }
            setTypingListener(object : MessageInput.TypingListener {
                override fun onStartTyping() {
                    viewModel.startTyping()
                }

                override fun onStopTyping() {
                    viewModel.endTyping()
                }
            })
        }

        val appCompatActivity = activity as? AppCompatActivity
        appCompatActivity?.setSupportActionBar(messageToolbar)
        NavigationUI.setupWithNavController(messageToolbar, findNavController())

        return root
    }
}
