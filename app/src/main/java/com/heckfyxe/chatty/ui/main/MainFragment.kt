package com.heckfyxe.chatty.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.heckfyxe.chatty.EmotionDetector
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.databinding.MainFragmentBinding
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.util.clearSharedPreferencesData
import com.heckfyxe.chatty.util.dp
import com.heckfyxe.chatty.util.setAuthenticated
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.sqrt

private val FAB_EXPANDING_RADIUS = 100f.dp

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModel()

    private lateinit var adapter: DialogsAdapter

    private lateinit var emotionDetector: EmotionDetector

    private lateinit var newMessageFAB: FloatingActionButton
    private lateinit var phoneFAB: FloatingActionButton
    private lateinit var contactsFAB: FloatingActionButton
    private lateinit var nicknameFAB: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAuthenticated()

        viewModel.refreshChats()

        adapter = DialogsAdapter { dialog ->
            viewModel.launchMessageFragment(dialog)
        }

        connectToViewModel()
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
                    findNavController().navigate(R.id.action_global_authFragment)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun connectToViewModel() {
        viewModel.errors.observe(this, Observer { exception ->
            exception?.let {
                viewModel.onErrorGotten()
                Log.e("MainFragment", it.message, it.cause)
                Toast.makeText(context!!, R.string.connection_error, Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.isFABExpanded.observe(this, Observer {
            it ?: return@Observer

            if (it) expandFAB()
            else resetFAB()
        })

        viewModel.launchMessagesEvent.observe(this, Observer {
            it ?: return@Observer

            viewModel.onMessageFragmentLaunched()
            launchMessageFragment(
                it.channelId,
                it.interlocutor,
                it.lastMessageTime
            )
        })
    }

    private fun expandFAB() {
        newMessageFAB.animate().rotation(45f)

        // Translating
        phoneFAB.animate().translationX(-FAB_EXPANDING_RADIUS)
        contactsFAB.animate().translationX(-sqrt(2f) / 2f * FAB_EXPANDING_RADIUS)
        contactsFAB.animate().translationY(-sqrt(2f) / 2f * FAB_EXPANDING_RADIUS)
        nicknameFAB.animate().translationY(-FAB_EXPANDING_RADIUS)

        // Visibility
        phoneFAB.isVisible = true
        contactsFAB.isVisible = true
        nicknameFAB.isVisible = true

        // Alpha
        phoneFAB.animate().alpha(1f)
        contactsFAB.animate().alpha(1f)
        nicknameFAB.animate().alpha(1f)
    }

    private fun resetFAB() {
        newMessageFAB.animate().rotation(0f)

        // Translating
        phoneFAB.animate().translationX(0f)
        contactsFAB.animate().translationX(0f)
        contactsFAB.animate().translationY(0f)
        nicknameFAB.animate().translationY(0f)

        // Alpha
        val goneAction: (View) -> Runnable = {
            Runnable { it.isGone = true }
        }
        phoneFAB.animate().alpha(0f).withEndAction(goneAction(phoneFAB))
        contactsFAB.animate().alpha(0f).withEndAction(goneAction(contactsFAB))
        nicknameFAB.animate().alpha(0f).withEndAction(goneAction(nicknameFAB))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = MainFragmentBinding.inflate(inflater).let {
        it.lifecycleOwner = this
        it.viewModel = viewModel

        newMessageFAB = it.newMessageFAB
        phoneFAB = it.phoneFAB
        contactsFAB = it.contactsFAB
        nicknameFAB = it.nicknameFAB

        phoneFAB.setOnClickListener {
            showNewInterlocutorByPhoneNumberDialog()
            resetFAB()
        }
        contactsFAB.setOnClickListener {
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToContactFragment())
            resetFAB()
        }
        nicknameFAB.setOnClickListener {
            showNewInterlocutorByNicknameDialog()
            resetFAB()
        }

        (activity as? AppCompatActivity)?.setSupportActionBar(it.mainToolbar)
        it.dialogList.setHasFixedSize(true)
        it.dialogList.adapter = adapter
        it.newMessageFAB.setOnClickListener { viewModel.addMessageFABClicked() }

        return it.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_CREATE_DIALOG -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null ||
                        !data.hasExtra(NewInterlocutorByUserDataDialog.EXTRA_CHANNEL_ID) ||
                        !data.hasExtra(NewInterlocutorByUserDataDialog.EXTRA_INTERLOCUTOR)
                    ) {
                        Log.w("MainFragment", "Not got requirement data from dialog")
                        return
                    }
                    val dialogId = data.getStringExtra(
                        NewInterlocutorByUserDataDialog.EXTRA_CHANNEL_ID
                    )!!
                    val user = data.getSerializableExtra(
                        NewInterlocutorByUserDataDialog.EXTRA_INTERLOCUTOR
                    ) as User
                    launchMessageFragment(dialogId, user)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showNewInterlocutorByPhoneNumberDialog() =
        showNewInterlocutorByUserData(NewInterlocutorByUserDataDialog.UserDataType.PHONE_NUMBER)


    private fun showNewInterlocutorByNicknameDialog() =
        showNewInterlocutorByUserData(NewInterlocutorByUserDataDialog.UserDataType.NICKNAME)


    private fun showNewInterlocutorByUserData(userDataType: NewInterlocutorByUserDataDialog.UserDataType) {
        NewInterlocutorByUserDataDialog.newInstance(userDataType).let {
            it.setTargetFragment(this@MainFragment, RC_CREATE_DIALOG)
            it.show(childFragmentManager, null)
        }
    }

    override fun onStart() {
        super.onStart()

        emotionDetector.start()
    }

    private fun launchMessageFragment(
        channelId: String,
        interlocutor: User,
        lastMessageTime: Long = -1
    ) {

        val direction = MainFragmentDirections.actionMainFragmentToMessageFragment(
            channelId,
            interlocutor,
            lastMessageTime
        )
        findNavController().navigate(direction)
    }

    companion object {
        private const val RC_CREATE_DIALOG = 0
    }
}
