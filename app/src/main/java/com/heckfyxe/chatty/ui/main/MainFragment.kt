package com.heckfyxe.chatty.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.ui.message.MessageFragment
import com.heckfyxe.chatty.util.GlideImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {

    private val model: MainViewModel by viewModel()

    private lateinit var adapter: DialogsListAdapter<Dialog>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = DialogsListAdapter(GlideImageLoader())
        adapter.setOnDialogClickListener {
            launchMessageFragment(it.channel.serialize())
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

        newMessageFAB?.setOnClickListener {
            NewDialogDialog().let {
                it.setTargetFragment(this@MainFragment, RC_CREATE_DIALOG)
                it.show(fragmentManager!!, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_CREATE_DIALOG -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.hasExtra(NewDialogDialog.EXTRA_CHANNEL) == true) {
                        launchMessageFragment(data.getByteArrayExtra(NewDialogDialog.EXTRA_CHANNEL))
                    } else {
                        Log.w("MainFragment", "Data doesn't have channel")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun launchMessageFragment(serializedGroupChannel: ByteArray) {
        findNavController().navigate(R.id.action_mainFragment_to_messageFragment, bundleOf(
            MessageFragment.ARG_CHANNEL to serializedGroupChannel
        ))
    }

    companion object {
        private const val RC_CREATE_DIALOG = 0
    }
}
