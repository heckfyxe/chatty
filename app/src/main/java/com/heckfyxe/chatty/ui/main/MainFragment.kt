package com.heckfyxe.chatty.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Dialog
import com.sendbird.android.BaseChannel
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {

    private val model: MainViewModel by viewModel()

    private lateinit var adapter: DialogsListAdapter<Dialog>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = DialogsListAdapter { imageView, url, _ ->
            Glide.with(imageView)
                .load(url)
                .into(imageView)
        }

        connectToViewModel()

        model.connectUser()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.mi_sign_out -> {
                model.logOut {
                    findNavController().navigate(R.id.action_mainFragment_to_authFragment)
                }
                true
            }
            R.id.mi_create_chat -> {
                NewConversationDialog().let {
                    it.setTargetFragment(this@MainFragment, RC_CREATE_DIALOG)
                    it.show(fragmentManager!!, null)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun connectToViewModel() {
        model.currentUser.observe(this, Observer {
            model.loadChats()
        })

        model.errors.observe(this, Observer {
            Log.e("MainFragment", it.message, it.cause)
            Toast.makeText(context!!, R.string.connection_error, Toast.LENGTH_SHORT).show()
        })

        model.chats.observe(this, Observer {
            it.forEach { channel ->
                adapter.upsertItem(Dialog(channel, model.userId))
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialogList?.setAdapter(adapter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_CREATE_DIALOG -> {
                if (resultCode == Activity.RESULT_OK) {
                    val channel = BaseChannel.buildFromSerializedData(
                        data!!.getByteArrayExtra(NewConversationDialog.EXTRA_CHANNEL))
                    channel.sendUserMessage("Hello") { _, e ->
                        if (e != null) {
                            throw Exception()
                        }
                    }
                    model.loadChats()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val RC_CREATE_DIALOG = 0
    }
}
