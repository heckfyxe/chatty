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
import com.google.firebase.iid.FirebaseInstanceId
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.ChatDialog
import com.heckfyxe.chatty.util.GlideImageLoader
import com.sendbird.android.SendBird
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainFragment : Fragment() {

    private val model: MainViewModel by viewModel()

    private lateinit var adapter: DialogsListAdapter<ChatDialog>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = DialogsListAdapter(GlideImageLoader())
        adapter.setOnDialogClickListener {
            launchMessageFragment(it.id)
        }

        connectToViewModel()

        model.connectUser()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(mainToolbar)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(com.heckfyxe.chatty.R.menu.fragment_main, menu)
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
            registerPushNotification()
        })

        model.errors.observe(this, Observer {
            Log.e("MainFragment", it.message, it.cause)
            Toast.makeText(context!!, com.heckfyxe.chatty.R.string.connection_error, Toast.LENGTH_SHORT).show()
        })

        model.chats.observe(this, Observer {
            adapter.setItems(it)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(com.heckfyxe.chatty.R.layout.main_fragment, container, false)
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
                    if (data?.hasExtra(NewDialogDialog.EXTRA_CHANNEL_ID) == true) {
                        launchMessageFragment(data.getStringExtra(NewDialogDialog.EXTRA_CHANNEL_ID))
                    } else {
                        Log.w("MainFragment", "Data doesn't have channel")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun registerPushNotification() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(activity!!) { instanceIdResult ->
            SendBird.registerPushTokenForCurrentUser(instanceIdResult.token) { _, e ->
                if (e != null)
                    Log.w("MainFragment", "registerPushNotification", e)
            }
        }
    }

    private fun launchMessageFragment(channelId: String) {
        val direction = MainFragmentDirections.actionMainFragmentToMessageFragment(channelId)
        findNavController().navigate(direction)
    }

    companion object {
        private const val RC_CREATE_DIALOG = 0
    }
}
