package com.heckfyxe.chatty.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.heckfyxe.chatty.R
import kotlinx.android.synthetic.main.edit_user_data_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditUserDataFragment : Fragment() {

    private val model: EditUserDataViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.currentUser.observe(this, Observer {
            Glide.with(this)
                .load(it.profileUrl)
                .into(circleImageView)

            nicknameEditText?.setText(it.nickname)
        })

        model.errors.observe(this, Observer {
            Toast.makeText(context!!, it.localizedMessage, Toast.LENGTH_SHORT).show()
        })

        model.connectUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.edit_user_data_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nicknameEditText?.addTextChangedListener {
            nicknameOkButton?.isEnabled = !it.isNullOrBlank()
        }

        nicknameEditText?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateUserData()
                return@setOnEditorActionListener true
            }
            false
        }

        nicknameOkButton?.setOnClickListener {
            updateUserData()
        }
    }

    private fun updateUserData() {
        if (!nicknameEditText?.text.isNullOrEmpty()) {
            model.updateUserData(nicknameEditText.text.toString()) {
                findNavController().navigate(R.id.action_editUserDataFragment_to_mainFragment)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState?.apply {
            nicknameEditText?.setText(getCharSequence(EXTRA_NICKNAME))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putCharSequence(EXTRA_NICKNAME, nicknameEditText?.text)
        }
    }

    companion object {
        private const val EXTRA_NICKNAME = "com.heckfyxe.chatty.EXTRA_NICKNAME"
    }
}
