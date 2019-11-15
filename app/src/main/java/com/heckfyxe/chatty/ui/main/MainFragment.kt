package com.heckfyxe.chatty.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.iid.FirebaseInstanceId
import com.heckfyxe.chatty.EmotionDetector
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.util.OnClickAction
import com.heckfyxe.chatty.util.clearSharedPreferencesData
import com.heckfyxe.chatty.util.setAuthenticated
import com.sendbird.android.SendBird
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModel()

    private lateinit var adapter: DialogsAdapter

    private lateinit var emotionDetector: EmotionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAuthenticated()

        adapter = DialogsAdapter(OnClickAction {
            viewModel.launchMessageFragment(it.id)
        })

        connectToViewModel()
    }

    private fun showUserConnectingAnimation() {
        mainProgressBar?.isVisible = true
        mainToolbar?.isVisible = false
        newMessageFAB?.isVisible = false
        dialogList?.isVisible = false
    }

    private fun hideUserConnectingAnimation() {
        mainProgressBar?.isVisible = false
        mainToolbar?.isVisible = true
        newMessageFAB?.isVisible = true
        dialogList?.isVisible = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(mainToolbar)
        setHasOptionsMenu(true)

        emotionDetector = activity as EmotionDetector
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.mi_add_friends -> {
                findNavController().navigate(R.id.action_mainFragment_to_contactFragment)
                true
            }
            R.id.mi_sign_out -> {
                viewModel.logOut {
                    clearSharedPreferencesData()
                    findNavController().navigate(R.id.action_mainFragment_to_authFragment)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun connectToViewModel() {
        viewModel.currentUser.observe(this, Observer {
            viewModel.loadChats()
            registerPushNotification()
        })

        viewModel.errors.observe(this, Observer { exception ->
            exception?.let {
                viewModel.errors.postValue(null)
                Log.e("MainFragment", it.message, it.cause)
                Toast.makeText(context!!, R.string.connection_error, Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.launchMessagesEvent.observe(this, Observer {
            it ?: return@Observer

            viewModel.onMessageFragmentLaunched()
            launchMessageFragment(it.channelId, it.interlocutor)
        })

        viewModel.chats.observe(this, Observer {
            hideUserConnectingAnimation()
            adapter.submitList(it)
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

        (activity as? AppCompatActivity)?.setSupportActionBar(mainToolbar)

        dialogList?.adapter = adapter

        viewModel.connectUser()
        showUserConnectingAnimation()

        newMessageFAB?.setOnClickListener { showNewInterlocutorDialog() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_CREATE_DIALOG -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.hasExtra(NewInterlocutorByUserDataDialog.EXTRA_CHANNEL_ID) == true) {
                        val dialogId = data.getStringExtra(
                            NewInterlocutorByUserDataDialog.EXTRA_CHANNEL_ID)!!
                        viewModel.launchMessageFragment(dialogId)
                    } else {
                        Log.w("MainFragment", "Data doesn't have channel")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showNewInterlocutorDialog() {
        AlertDialog.Builder(context!!)
            .setItems(R.array.new_dialog_methods) { _, position ->
                when (position) {
                    0 -> showNewInterlocutorFromFriends() // From friends
                    1 -> showNewInterlocutorByPhoneNumberDialog() // By phone number

                    2 -> showNewInterlocutorByNicknameDialog() // By nickname
                }
            }
            .create()
            .show()
    }

    private fun showNewInterlocutorFromFriends() {
        findNavController().navigate(R.id.action_mainFragment_to_friendsFragment)
    }

    private fun showNewInterlocutorByPhoneNumberDialog() =
        showNewInterlocutorByUserData(NewInterlocutorByUserDataDialog.UserDataType.PHONE_NUMBER)


    private fun showNewInterlocutorByNicknameDialog() =
        showNewInterlocutorByUserData(NewInterlocutorByUserDataDialog.UserDataType.NICKNAME)


    private fun showNewInterlocutorByUserData(userDataType: NewInterlocutorByUserDataDialog.UserDataType) {
        NewInterlocutorByUserDataDialog.newInstance(userDataType).let {
            it.setTargetFragment(this@MainFragment, RC_CREATE_DIALOG)
            it.show(fragmentManager!!, null)
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

    override fun onStart() {
        super.onStart()

        emotionDetector.start()
    }

    private fun launchMessageFragment(channelId: String, interlocutor: User) {
        val direction = MainFragmentDirections.actionMainFragmentToMessageFragment(
            channelId,
            interlocutor
        )
        findNavController().navigate(direction)
    }

    companion object {
        private const val RC_CREATE_DIALOG = 0
    }
}
