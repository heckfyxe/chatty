package com.heckfyxe.chatty.ui.message

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.transition.TransitionInflater
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.databinding.MessageFragmentBinding
import com.heckfyxe.chatty.model.User
import com.stfalcon.chatkit.messages.MessageInput
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val RC_GET_IMAGE = 0

class MessageFragment : Fragment() {

    private val viewModel: MessageViewModel by viewModel {
        parametersOf(
            args.channelId,
            if (args.user != User.DELETED) args.user.id else null,
            args.lastMessageTime
        )
    }

    private val args: MessageFragmentArgs by navArgs()

    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!)
                .inflateTransition(android.R.transition.move)
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.scrollDown.observe(this, Observer { needsScrollDown ->
            if (needsScrollDown) {
                layoutManager.scrollToPosition(0)
                viewModel.onScrolledDown()
            }
        })

        viewModel.deleteImageFile.observe(this, Observer {
            it ?: return@Observer

            it.delete()
            viewModel.onImageFileDeleted()
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
        dialogId = args.channelId
        interlocutor = if (args.user != User.DELETED) args.user else User("", "DELETED", "")
        messageViewModel = viewModel
        executePendingBindings()

        layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, true)
        messageList.setHasFixedSize(true)
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
            setAttachmentsListener {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(
                        Intent.createChooser(this, getString(R.string.select_image)),
                        RC_GET_IMAGE
                    )
                }
            }
        }

        val appCompatActivity = activity as? AppCompatActivity
        appCompatActivity?.setSupportActionBar(messageToolbar)
        NavigationUI.setupWithNavController(messageToolbar, findNavController())

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_GET_IMAGE -> {
                val file = data?.data?.toJpegImageFile() ?: return
                viewModel.sendImageMessage(file)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Throws(IOException::class)
    private fun createFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(context!!.cacheDir, timeStamp)
    }

    @Throws(IOException::class)
    private fun Uri.toJpegImageFile(): File? {
        val file = createFile()
        val bitmap = context!!.contentResolver.openInputStream(this)?.use {
            BitmapFactory.decodeStream(it)
        }
        file.outputStream().use {
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, it)
        }
        return file
    }
}
