package com.heckfyxe.chatty.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.heckfyxe.chatty.R
import org.koin.android.ext.android.inject

class ContactFragment : Fragment() {

    companion object {
        private const val RC_READ_CONTACTS_PERMISSION = 0
    }

    private val viewModel: ContactViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startProcess()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), RC_READ_CONTACTS_PERMISSION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.contact_fragment, container, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RC_READ_CONTACTS_PERMISSION -> {
                val index = permissions.indexOf(Manifest.permission.READ_CONTACTS)
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    startProcess()
                } else {
                    skipProcess()
                }
            }
        }
    }

    private fun startProcess() {
        viewModel.getUsers()
    }

    private fun skipProcess() {
        findNavController().navigate(R.id.action_contactFragment_to_mainFragment)
    }

}
