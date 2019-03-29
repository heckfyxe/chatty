package com.heckfyxe.chatty.ui.main

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.heckfyxe.chatty.R
import kotlinx.android.synthetic.main.dialog_new_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class NewDialogDialog: DialogFragment() {

    private val model: NewDialogViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.init()
        observeViewModel()
    }

    private fun observeViewModel() {
        model.errors.observe(this, Observer {
            showError()
        })

        model.result.observe(this, Observer {
            if (it.data == userDataEditText?.text.toString()) {
                userDataCheckingProgressBar?.isVisible = false

                if (it.userId == null) {
                    showNotFound()
                } else {
                    showSuccessful()
                    createDialog(it.userId)
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_new_dialog, container, false)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        dialog.setTitle(R.string.new_dialog)
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
    }

    private fun showError() {
        userDataCheckingResultTextView?.apply {
            setTextColor(resources.getColor(R.color.error))
            text = resources.getText(R.string.error)
        }
    }

    private fun showNotFound() {
        userDataCheckingResultTextView?.apply {
            setTextColor(resources.getColor(R.color.not_found))
            text = resources.getText(R.string.not_found)
        }
    }

    private fun showSuccessful() {
        userDataCheckingResultTextView?.apply {
            setTextColor(resources.getColor(R.color.green))
            text = resources.getText(R.string.successful)
        }
    }

    private fun hideUserDataCheckingNotification() {
        userDataCheckingResultTextView?.text = null
    }

    private fun createDialog(userId: String) {
        model.createDialog(userId) {
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_CHANNEL, it.serialize())
            })
            dismiss()
        }
    }

    companion object {
        const val EXTRA_CHANNEL = "com.heckfyxe.chatty.ui.main.EXTRA_CHANNEL"
    }
}