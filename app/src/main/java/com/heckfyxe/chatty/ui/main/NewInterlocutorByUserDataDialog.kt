package com.heckfyxe.chatty.ui.main

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.util.sendbird.saveOnDevice
import kotlinx.android.synthetic.main.new_interlocutor_by_user_data_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NewInterlocutorByUserDataDialog private constructor() : DialogFragment() {

    private val model: NewInterlocutorByUserDataViewModel by viewModel { parametersOf(getDataType().typeName) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeViewModel()
    }

    private fun observeViewModel() {
        model.errors.observe(this, Observer {
            showError()
        })

        model.result.observe(this, Observer {
            if (it.data == userDataEditText?.text.toString()) {
                userDataCheckingProgressBar?.isVisible = false

                when (it.userId) {
                    null -> showNotFound()
                    model.userId -> showItsYourNumber()
                    else -> {
                        showSuccessful()
                        createDialog(it.userId)
                    }
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.new_interlocutor_by_user_data_dialog, container, false)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        okButton?.setOnClickListener {
            hideUserDataCheckingNotification()
            userDataCheckingProgressBar?.isVisible = true

            model.checkData(userDataEditText?.text.toString())
        }

        cancelButton?.setOnClickListener {
            dismiss()
        }



        userDataInputLayout?.hint = getString(
            when (getDataType()) {
                UserDataType.PHONE_NUMBER -> R.string.enter_phone_number
                UserDataType.NICKNAME -> R.string.enter_nickname
            }
        )

        if (getDataType() == UserDataType.PHONE_NUMBER) {
            userDataEditText?.inputType = InputType.TYPE_CLASS_PHONE
        }
    }

    private fun getDataType(): UserDataType {
        val type = arguments?.getString(ARG_USER_DATA_TYPE)
        return UserDataType.valueOf(type!!)
    }

    private fun showError() {
        userDataCheckingResultTextView?.apply {
            setTextColor(ContextCompat.getColor(context!!, R.color.error))
            text = resources.getText(R.string.error)
        }
    }

    private fun showNotFound() {
        userDataCheckingResultTextView?.apply {
            setTextColor(ContextCompat.getColor(context!!, R.color.warning))
            text = resources.getText(R.string.not_found)
        }
    }

    private fun showSuccessful() {
        userDataCheckingResultTextView?.apply {
            setTextColor(ContextCompat.getColor(context!!, R.color.green))
            text = resources.getText(R.string.successful)
        }
    }

    private fun showItsYourNumber() {
        userDataCheckingResultTextView?.apply {
            setTextColor(ContextCompat.getColor(context!!, R.color.warning))
            text = getString(R.string.its_your)
        }
    }

    private fun hideUserDataCheckingNotification() {
        userDataCheckingResultTextView?.text = null
    }

    private fun createDialog(userId: String) {
        model.createDialog(userId) {
            it.saveOnDevice()
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_CHANNEL_ID, it.url)
            })
            dismiss()
        }
    }

    enum class UserDataType(val typeName: String) {
        PHONE_NUMBER("phoneNumber"), NICKNAME("nickname");
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "com.heckfyxe.chatty.ui.main.EXTRA_CHANNEL_ID"

        private const val ARG_USER_DATA_TYPE = "com.heckfyxe.chatty.ui.main.ARG_USER_DATA_TYPE"

        fun newInstance(type: UserDataType) =
            NewInterlocutorByUserDataDialog().apply {
                arguments = bundleOf(ARG_USER_DATA_TYPE to type.toString())
            }
    }
}